/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams.part1b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AsyncSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs asynchronously (on an Executor), requests one element
 * at a time, and invokes a user-defined method to process each element.
 *
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

    private interface Signal extends Runnable {
    }

    private class OnSubscribe implements Signal {
        public final Flow.Subscription subscription;

        public OnSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void run() {
            handleOnSubscribe(subscription);
        }
    }

    private class OnNext implements Signal {
        public final T element;

        public OnNext(T element) {
            this.element = element;
        }

        @Override
        public void run() {
            if (!done) {
                if (whenNext(element)) {
                    subscription.request(1);
                } else {
                    done();
                }
            }
        }
    }

    private class OnError implements Signal {
        public final Throwable throwable;

        public OnError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void run() {
            done = true;
            whenError(throwable);
        }
    }

    private class OnComplete implements Signal {

        @Override
        public void run() {
            done = true;
            whenComplete();
        }
    }

    private final Executor executor;

    private Flow.Subscription subscription;
    private boolean done;

    protected AsyncSubscriber(Executor executor) {
        this.executor = executor;
    }

    private void done() {
        done = true;
        subscription.cancel();
    }

    protected void whenError(Throwable throwable) {
    }

    protected abstract boolean whenNext(T element);

    protected void whenComplete() {
    }

    private void handleOnSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
        } else {
            this.subscription = subscription;
            this.subscription.request(1);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("subscriber.subscribe: {}", subscription);
        if (subscription == null) {
            throw new NullPointerException();
        }

        signal(new OnSubscribe(subscription));
    }

    @Override
    public void onNext(T element) {
        logger.info("subscriber.next: {}", element);
        if (element == null) {
            throw new NullPointerException();
        }

        signal(new OnNext(element));
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("subscriber.error", throwable);
        if (throwable == null) {
            throw new NullPointerException();
        }

        signal(new OnError(throwable));
    }

    @Override
    public void onComplete() {
        logger.info("subscriber.complete");
        signal(new OnComplete());
    }

    // This `ConcurrentLinkedQueue` will track signals that are sent to this `Subscriber`, like `OnComplete` and `OnNext` ,
    // and obeying rule 2.11
    private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<Signal>();

    // We are using this `AtomicBoolean` to make sure that this `Subscriber` doesn't run concurrently with itself,
    // obeying rule 2.7 and 2.11
    private final AtomicBoolean on = new AtomicBoolean(false);

//    @SuppressWarnings("unchecked")
    @Override
    public final void run() {
        if (on.get()) { // establishes a happens-before relationship with the end of the previous run
            try {
                final Signal s = inboundSignals.poll(); // We take a signal off the queue
                if (!done) { // If we're done, we shouldn't process any more signals, obeying rule 2.8
                    s.run();
//                    // Below we simply unpack the `Signal`s and invoke the corresponding methods
//                    if (s instanceof OnNext<?>)
//                        handleOnNext(((OnNext<T>) s).next);
//                    else if (s instanceof OnSubscribe)
//                        handleOnSubscribe(((OnSubscribe) s).subscription);
//                    else if (s instanceof OnError) // We are always able to handle OnError, obeying rule 2.10
//                        handleOnError(((OnError) s).error);
//                    else if (s instanceof OnComplete) // We are always able to handle OnComplete, obeying rule 2.9
//                        handleOnComplete();
                }
            } finally {
                on.set(false); // establishes a happens-before relationship with the beginning of the next run
                if (!inboundSignals.isEmpty()) // If we still have signals to process
                    tryScheduleToExecute(); // Then we try to schedule ourselves to execute again
            }
        }
    }

    // What `signal` does is that it sends signals to the `Subscription` asynchronously
    private void signal(final Signal signal) {
        if (inboundSignals.offer(signal)) // No need to null-check here as ConcurrentLinkedQueue does this for us
            tryScheduleToExecute(); // Then we try to schedule it for execution, if it isn't already
    }

    // This method makes sure that this `Subscriber` is only executing on one Thread at a time
    private final void tryScheduleToExecute() {
        if (on.compareAndSet(false, true)) {
            try {
                executor.execute(this);
            } catch (Throwable t) { // If we can't run on the `Executor`, we need to fail gracefully and not violate rule 2.13
                if (!done) {
                    try {
                        done(); // First of all, this failure is not recoverable, so we need to cancel our subscription
                    } finally {
                        inboundSignals.clear(); // We're not going to need these anymore
                        // This subscription is cancelled by now, but letting the Subscriber become schedulable again means
                        // that we can drain the inboundSignals queue if anything arrives after clearing
                        on.set(false);
                    }
                }
            }
        }
    }
}
