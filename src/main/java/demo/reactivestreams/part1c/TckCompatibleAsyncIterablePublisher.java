package demo.reactivestreams.part1c;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * AsyncIterablePublisher is an implementation of Reactive Streams `Publisher`
 * which executes asynchronously, using a provided `Executor` and produces elements
 * from a given `Iterable` in a "unicast" configuration to its `Subscribers`.
 * <p>
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public class TckCompatibleAsyncIterablePublisher<T> implements Flow.Publisher<T> {

    private final static int DEFAULT_BATCHSIZE = 1024;

    private final Supplier<Iterator<T>> iteratorSupplier; // This is our data source / generator
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
    public void subscribe(final Flow.Subscriber<? super T> s) {
        // As per rule 1.11, we have decided to support multiple subscribers in a unicast configuration
        // for this `Publisher` implementation.
        // As per 2.13, this method must return normally (i.e. not throw)
        new SubscriptionImpl(s).init();
    }

    private class SubscriptionImpl implements Flow.Subscription, Runnable {

        private final Flow.Subscriber<? super T> subscriber; // We need a reference to the `Subscriber` so we can talk to it

        private Iterator<T> iterator; // This is our cursor into the data stream, which we will send to the `Subscriber`
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

        // This method will register inbound demand from our `Subscriber` and validate it against rule 3.9 and rule 3.17
        private void doRequest(final long n) {
            if (n < 1) {
                doTerminate(new IllegalArgumentException(subscriber + " violated the Reactive Streams rule 3.9 by requesting a non-positive number of elements."));
            } else if (demand + n < 1) {
                demand = Long.MAX_VALUE;
                doSend();
            } else {
                demand += n;
                doSend();
            }
        }

        // This is our behavior for producing elements downstream
        private void doSend() {
            // In order to play nice with the `Executor` we will only send at-most `batchSize` before
            // rescheduing ourselves and relinquishing the current thread.
            int leftInBatch = batchSize;
            do {
                T next;
                boolean hasNext;
                try {
                    next = iterator.next(); // We have already checked `hasNext` when subscribing, so we can fall back to testing -after- `next` is called.
                    hasNext = iterator.hasNext(); // Need to keep track of End-of-Stream
                } catch (final Throwable t) {
                    doTerminate(t); // If `next` or `hasNext` throws (they can, since it is user-provided), we need to treat the stream as errored as per rule 1.4
                    return;
                }
                subscriber.onNext(next); // Then we signal the next element downstream to the `Subscriber`
                if (!hasNext) { // If we are at End-of-Stream
                    doCancel(); // We need to consider this `Subscription` as cancelled as per rule 1.6
                    subscriber.onComplete(); // Then we signal `onComplete` as per rule 1.2 and 1.5
                }
            } while (!cancelled           // This makes sure that rule 1.8 is upheld, i.e. we need to stop signalling "eventually"
                && --leftInBatch > 0 // This makes sure that we only send `batchSize` number of elements in one go (so we can yield to other Runnables)
                && --demand > 0);    // This makes sure that rule 1.1 is upheld (sending more than was demanded)

            if (!cancelled && demand > 0) // If the `Subscription` is still alive and well, and we have demand to satisfy, we signal ourselves to send more data
                signal(new Send());
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
//                        if (signal instanceof Request)
//                            doRequest(((Request) signal).n);
//                        else if (signal instanceof Send)
//                            doSend();
//                        else if (signal instanceof Cancel)
//                            doCancel();
//                        else if (signal instanceof Subscribe)
//                            doSubscribe();
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
                } catch (Throwable t) { // If we can't run on the `Executor`, we need to fail gracefully
                    if (!cancelled) {
                        doCancel(); // First of all, this failure is not recoverable, so we need to follow rule 1.4 and 1.6
                        try {
                            doTerminate(new IllegalStateException("Publisher terminated due to unavailable Executor.", t));
                        } finally {
                            inboundSignals.clear(); // We're not going to need these anymore
                            // This subscription is cancelled by now, but letting it become schedulable again means
                            // that we can drain the inboundSignals queue if anything arrives after clearing
                            mutex.set(false);
                        }
                    }
                }
            }
        }

        // The reason for the `init` method is that we want to ensure the `SubscriptionImpl`
        // is completely constructed before it is exposed to the thread pool, therefor this
        // method is only intended to be invoked once, and immediately after the constructor has
        // finished.
        void init() {
            signal(new Subscribe());
        }

        @Override
        public void request(final long n) {
            signal(new Request(n));
        }

        @Override
        public void cancel() {
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
            final long n;

            Request(final long n) {
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