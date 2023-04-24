package demo.reactivestreams.demo2;

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

    private class SubscriptionImpl implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final ExecutorImpl executorImpl;

        private Iterator<T> iterator;
        private long demand = 0;
        private boolean cancelled = false;

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = Objects.requireNonNull(subscriber);
            this.executorImpl = new ExecutorImpl();
        }

        private void doSubscribe() {
            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable throwable) {
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

            if (!cancelled) {
                subscriber.onSubscribe(this);

                if (!iterator.hasNext()) {
                    doCancel();
                    subscriber.onComplete();
                }
            }
        }

        private void doRequest(long n) {
            if (n < 1) {
                doError(new IllegalArgumentException("non-positive subscription request"));
            } else if (demand + n < 1) {
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
                subscriber.onNext(iterator.next());

                if (!iterator.hasNext()) {
                    doCancel();
                    subscriber.onComplete();
                }
            } while (!cancelled && --batchLeft > 0 && --demand > 0);

            if (!cancelled && demand > 0) {
                executorImpl.signal(new Next());
            }
        }

        private void doCancel() {
            cancelled = true;
        }

        private void doError(Throwable throwable) {
            cancelled = true;
            subscriber.onError(throwable);
        }

        void init() {
            executorImpl.signal(new Subscribe());
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);
            executorImpl.signal(new Request(n));
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            executorImpl.signal(new Cancel());
        }

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

        private class ExecutorImpl implements Runnable {

            private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<>();
            private final AtomicBoolean mutex = new AtomicBoolean(false);

            private void signal(Signal signal) {
                if (inboundSignals.offer(signal)) {
                    tryExecute();
                }
            }

            @Override
            public void run() {
                if (mutex.get()) {
                    try {
                        Signal signal = inboundSignals.poll();
                        if (!cancelled) {
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
                        if (!cancelled) {
                            doCancel();
                            try {
                                doError(new IllegalStateException(throwable));
                            } finally {
                                mutex.set(false);
                            }
                        }
                    }
                }
            }
        }
    }
}
