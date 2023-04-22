package demo.reactivestreams.part1c;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TckCompatibleAsyncIterablePublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(TckCompatibleAsyncIterablePublisher.class);

    private final static int DEFAULT_BATCHSIZE = 1024;

    private final Supplier<Iterator<T>> iteratorSupplier;
    private final Executor executor; // This is our thread pool, which will make sure that our Publisher runs asynchronously to its Subscribers
    private final int batchSize; // In general, if one uses an `Executor`, one should be nice nad not hog a thread for too long, this is the cap for that, in elements

    public TckCompatibleAsyncIterablePublisher(Supplier<Iterator<T>> iteratorSupplier, final Executor executor) {
        this(iteratorSupplier, DEFAULT_BATCHSIZE, executor);
    }

    public TckCompatibleAsyncIterablePublisher(Supplier<Iterator<T>> iteratorSupplier, final int batchSize, final Executor executor) {
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
        private long demand = 0; // Here we track the current demand, i.e. what has been requested but not yet delivered
        private boolean cancelled = false; // This flag will track whether this `Subscription` is to be considered cancelled or not

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
                doTerminate(throwable);
            }

            if (!cancelled) {
                subscriber.onSubscribe(this);

                boolean hasNext = false;
                try {
                    hasNext = iterator.hasNext();
                } catch (Throwable throwable) {
                    doTerminate(throwable);
                }

                if (!hasNext) {
                    doCancel();
                    subscriber.onComplete();
                }
            }
        }

        private void doRequest(long n) {
            if (n < 1) {
                doTerminate(new IllegalArgumentException("non-positive subscription request"));
            } else if (demand + n < 1) {
                demand = Long.MAX_VALUE;
                doSend();
            } else {
                demand += n;
                doSend();
            }
        }

        private void doSend() {
            int leftInBatch = batchSize;
            do {
                T next;
                boolean hasNext;
                try {
                    next = iterator.next(); // We have already checked `hasNext` when subscribing, so we can fall back to testing -after- `next` is called.
                    hasNext = iterator.hasNext(); // Need to keep track of End-of-Stream
                } catch (Throwable throwable) {
                    doTerminate(throwable);
                    return;
                }
                subscriber.onNext(next);

                if (!hasNext) {
                    doCancel();
                    subscriber.onComplete();
                }
            } while (!cancelled           // This makes sure that rule 1.8 is upheld, i.e. we need to stop signalling "eventually"
                && --leftInBatch > 0 // This makes sure that we only send `batchSize` number of elements in one go (so we can yield to other Runnables)
                && --demand > 0);    // This makes sure that rule 1.1 is upheld (sending more than was demanded)

            if (!cancelled && demand > 0) {
                signal(new Send());
            }
        }

        private void doCancel() {
            cancelled = true;
        }

        private void doTerminate(Throwable throwable) {
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
                            doTerminate(new IllegalStateException(throwable));
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

        private class Send implements Signal {
            @Override
            public void run() {
                doSend();
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
