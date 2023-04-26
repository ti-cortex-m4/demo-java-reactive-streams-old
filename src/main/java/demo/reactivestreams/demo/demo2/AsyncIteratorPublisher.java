package demo.reactivestreams.demo.demo2;

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
        // by rule 1.11, A Publisher MAY support multiple Subscribers and decides whether each Subscription is unicast or multicast (unicast).
        new SubscriptionImpl(subscriber).init();
    }

    private class SubscriptionImpl implements Flow.Subscription, Runnable {

        private final Flow.Subscriber<? super T> subscriber;

        private Iterator<T> iterator;
        private long demand = 0;
        private boolean cancelled = false;

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            // ??? by rule 1.9, a `Publisher.subscribe` must throw a `java.lang.NullPointerException` if the `Subscriber` is `null`
            this.subscriber = Objects.requireNonNull(subscriber);
        }

        private void doSubscribe() {
            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable throwable) {
                // ??? by rule 1.9, a `Subscription` must signal `onSubscribe` before `onError` if method `subscribe` fails
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void request(long n) {
                    }
                });
                // by rule 1.4, if a Publisher fails it must signal an onError.
                doError(throwable);
            }

            if (!cancelled) {
                subscriber.onSubscribe(this);

                boolean hasNext = false;
                try {
                    hasNext = iterator.hasNext();
                } catch (Throwable throwable) {
                    // by rule 1.4, if a Publisher fails it must signal an onError.
                    doError(throwable);
                }

                if (!hasNext) {
                    doCancel();
                    subscriber.onComplete();
                }
            }
        }

        private void doRequest(long n) {
            if (n <= 0) {
                // by rule 3.9, While the Subscription is not cancelled, Subscription.request(long n) must signal onError with a java.lang.IllegalArgumentException if the argument is <= 0.
                doError(new IllegalArgumentException("non-positive subscription request"));
            } else if (demand + n <= 0) {
                // by rule 3.17, a `Subscription` must support a demand up to `java.lang.Long.MAX_VALUE`
                demand = Long.MAX_VALUE;
                doNext();
            } else {
                // by rule 3.8, While the Subscription is not cancelled, Subscription.request(long n) must register the given number of additional elements to be produced to the respective subscriber.
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
                    // by rule 1.4, if a Publisher fails it must signal an onError.
                    doError(throwable);
                    return;
                }
                subscriber.onNext(next);

                if (!hasNext) {
                    // by rule 1.6, If a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
                    doCancel();
                    // by rule 1.5, If a Publisher terminates successfully it must signal an onComplete.
                    subscriber.onComplete();
                }
            } while (!cancelled && --batchLeft > 0 && --demand > 0);

            if (!cancelled && demand > 0) {
                signal(new Next());
            }
        }

        private void doCancel() {
            logger.debug("subscription.cancel");
            cancelled = true;
        }

        private void doError(Throwable throwable) {
            // by rule 1.6, If a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
            cancelled = true;
            subscriber.onError(throwable);
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

        // unbounded thread-safe queue for passing signals to be executed on other threads
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
            // by rule 2.7, a Subscriber must ensure that all calls on its Subscription's request and cancel methods are performed serially.
            if (mutex.get()) {
                try {
                    Signal signal = inboundSignals.poll();
                    logger.debug("signal.poll {}", signal);
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
                            // by rule 1.4, if a Publisher fails it must signal an onError.
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
