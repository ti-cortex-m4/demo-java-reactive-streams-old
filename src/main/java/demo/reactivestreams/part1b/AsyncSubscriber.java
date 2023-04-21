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
 * <p>
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

    private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean mutex = new AtomicBoolean(false);

    @Override
    public final void run() {
        if (mutex.get()) {
            try {
                Signal signal = inboundSignals.poll();
                if (!done) {
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

    private void signal(Signal signal) {
        if (inboundSignals.offer(signal)) {
            tryExecute();
        }
    }

    private void tryExecute() {
        if (mutex.compareAndSet(false, true)) {
            try {
                executor.execute(this);
            } catch (Throwable throwable) {
                if (!done) {
                    try {
                        done();
                    } finally {
                        inboundSignals.clear();
                        mutex.set(false);
                    }
                }
            }
        }
    }
}
