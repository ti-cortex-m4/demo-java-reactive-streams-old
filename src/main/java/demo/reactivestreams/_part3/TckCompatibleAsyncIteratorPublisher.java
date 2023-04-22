package demo.reactivestreams._part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TckCompatibleAsyncIteratorPublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(TckCompatibleAsyncIteratorPublisher.class);

    private final static int DEFAULT_BATCHSIZE = 1024;

    private final Supplier<Iterator<T>> iteratorSupplier;
    private final Executor executor; // This is our thread pool, which will make sure that our Publisher runs asynchronously to its Subscribers
    private final int batchSize; // In general, if one uses an `Executor`, one should be nice nad not hog a thread for too long, this is the cap for that, in elements

    public TckCompatibleAsyncIteratorPublisher(Supplier<Iterator<T>> iteratorSupplier, final Executor executor) {
        this(iteratorSupplier, DEFAULT_BATCHSIZE, executor);
    }

    public TckCompatibleAsyncIteratorPublisher(Supplier<Iterator<T>> iteratorSupplier, final int batchSize, final Executor executor) {
        if (iteratorSupplier == null) {
            throw new NullPointerException();
        }
        if (executor == null) {
            throw new NullPointerException();
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException();
        }
        this.iteratorSupplier = iteratorSupplier;
        this.executor = executor;
        this.batchSize = batchSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> s) {
        new SubscriptionImpl(s).init();
    }

    private class SubscriptionImpl implements Flow.Subscription, Runnable {

        private final Flow.Subscriber<? super T> subscriber; // We need a reference to the `Subscriber` so we can talk to it

        private Iterator<T> iterator;
        private long demand = 0;
        private boolean cancelled = false;

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            if (subscriber == null) {
                throw new NullPointerException();
            }
            this.subscriber = subscriber;
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

                boolean hasNext = false;
                try {
                    hasNext = iterator.hasNext();
                } catch (Throwable throwable) {
                    doError(throwable);
                }

                if (!hasNext) {
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
                doSendBatch();
            } else {
                demand += n;
                doSendBatch();
            }
        }

        private void doSendBatch() {
            int leftInBatch = batchSize;
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
                    doCancel();
                    subscriber.onComplete();
                }
            } while (!cancelled && --leftInBatch > 0 && --demand > 0);

            if (!cancelled && demand > 0) {
                signal(new SendBatch());
            }
        }

        private void doCancel() {
            cancelled = true;
        }

        private void doError(Throwable throwable) {
            cancelled = true;
            subscriber.onError(throwable);
        }

        private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<Signal>();
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

        void init() {
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

        private class SendBatch implements Signal {
            @Override
            public void run() {
                doSendBatch();
            }
        }

        private class Cancel implements Signal {
            @Override
            public void run() {
                doCancel();
            }
        }
    }
}
