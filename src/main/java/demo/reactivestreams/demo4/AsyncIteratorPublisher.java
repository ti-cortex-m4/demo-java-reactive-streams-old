package demo.reactivestreams.demo4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AsyncIteratorPublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncIteratorPublisher.class);

    private final Supplier<Iterator<T>> iteratorSupplier;
    private final Executor executor;
    private final int batchSize;

    public AsyncIteratorPublisher(Supplier<Iterator<T>> iteratorSupplier, int batchSize, Executor executor) {
        if (batchSize < 1) {
            throw new IllegalArgumentException();
        }
        this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
        this.executor = Objects.requireNonNull(executor);
        this.batchSize = batchSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        new SubscriptionImpl(subscriber).init();
    }

    private class SubscriptionImpl implements Flow.Subscription, Runnable {

        private final Flow.Subscriber<? super T> subscriber;

        private Iterator<T> iterator;
        private long demand = 0;
        private boolean terminated = false;

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            // by rule 1.9, a `Subscription` must throw a `java.lang.NullPointerException` if the `Subscriber` is `null`
            this.subscriber = Objects.requireNonNull(subscriber);
        }

        private void doSubscribe() {
            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable throwable) {
                // by rule 1.9, a `Subscription` must signal `onSubscribe` before `onError` if method `subscribe` fails
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void request(long n) {
                    }
                });
                doError(throwable);
            }

            if (!terminated) {
                subscriber.onSubscribe(this);

                boolean hasNext = false;
                try {
                    hasNext = iterator.hasNext();
                } catch (Throwable throwable) {
                    doError(throwable);
                }

                if (!hasNext) {
                    doTerminate();
                    subscriber.onComplete();
                }
            }
        }

        private void doRequest(long n) {
            if (n <= 0) {
                // by rule 3.9, `Subscription.request` must signal `onError` with a `java.lang.IllegalArgumentException` if the argument is <= 0
                doError(new IllegalArgumentException("non-positive subscription request"));
            } else if (demand + n <= 0) {
                demand = Long.MAX_VALUE;
                doNext();
            } else {
                demand += n;
                doNext();
            }
        }

        private void doNext() {
            int batchLeft = batchSize;
            do {
                T next;
                boolean hasNext;
                try {
                    next = iterator.next();
                    hasNext = iterator.hasNext();
                } catch (Throwable throwable) {
                    doError(throwable);
                    return;
                }
                subscriber.onNext(next);

                if (!hasNext) {
                    doTerminate();
                    subscriber.onComplete();
                }
            } while (!terminated && --batchLeft > 0 && --demand > 0);

            if (!terminated && demand > 0) {
                signal(new Next());
            }
        }

        private void doCancel() {
            doTerminate();
        }

        private void doError(Throwable throwable) {
            doTerminate();
            subscriber.onError(throwable);
        }

        private void doTerminate() {
            logger.debug("subscription.terminate");
            terminated = true;
        }

        private void init() {
            signal(new Subscribe());
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);
            signal(new Request(n));
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            signal(new Cancel());
        }

        // `Signal` represents the asynchronous protocol between the `Publisher` and `Subscriber`
        private interface Signal extends Runnable {
        }

        private class Subscribe implements Signal {
            @Override
            public void run() {
                doSubscribe();
            }
        }

        private class Request implements Signal {
            private final long n;

            Request(long n) {
                this.n = n;
            }

            @Override
            public void run() {
                doRequest(n);
            }
        }

        private class Next implements Signal {
            @Override
            public void run() {
                doNext();
            }
        }

        private class Cancel implements Signal {
            @Override
            public void run() {
                doCancel();
            }
        }

        private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean mutex = new AtomicBoolean(false);

        private void signal(Signal signal) {
            logger.debug("signal.offer {}", signal);
            if (inboundSignals.offer(signal)) {
                tryExecute();
            }
        }

        @Override
        public void run() {
            if (mutex.get()) {
                try {
                    Signal signal = inboundSignals.poll();
                    logger.debug("signal.poll {}", signal);
                    if (!terminated) {
                        signal.run();
                    }
                } finally {
                    mutex.set(false);
                    if (!inboundSignals.isEmpty()) {
                        tryExecute();
                    }
                }
            }
        }

        private void tryExecute() {
            if (mutex.compareAndSet(false, true)) {
                try {
                    executor.execute(this);
                } catch (Throwable throwable) {
                    if (!terminated) {
                        doTerminate();
                        try {
                            doError(new IllegalStateException(throwable));
                        } finally {
                            inboundSignals.clear();
                            mutex.set(false);
                        }
                    }
                }
            }
        }
    }
}
