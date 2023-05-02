package demo.reactivestreams.demo2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

    private final int id;
    private final CountDownLatch completed = new CountDownLatch(1);
    private final Executor executor;

    private Flow.Subscription subscription;
    private boolean cancelled = false;

    public AsyncSubscriber(int id, Executor executor) {
        this.id = id;
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        // By rule 2.13, calling onSubscribe must throw a NullPointerException when the given parameter is null.
        signal(new OnSubscribe(Objects.requireNonNull(subscription)));
    }

    @Override
    public void onNext(T item) {
        logger.info("({}) subscriber.next: {}", id, item);
        // By rule 2.13, calling onNext must throw a NullPointerException when the given parameter is null.
        signal(new OnNext(Objects.requireNonNull(item)));
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
        // By rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
        signal(new OnError(Objects.requireNonNull(throwable)));
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);
        signal(new OnComplete());
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    // This method is invoked when OnNext signals arrive and returns whether more elements are desired or not (is intended to override).
    protected boolean whenNext(T item) {
        return true;
    }

    // This method is invoked when an OnError signal arrives (is intended to override).
    protected void whenError(Throwable throwable) {
    }

    // This method is invoked when an OnComplete signal arrives (is intended to override).
    protected void whenComplete() {
        completed.countDown();
    }

    private void doSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            // By rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
            subscription.cancel();
        } else {
            this.subscription = subscription;
            // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
            this.subscription.request(1);
        }
    }

    private void doNext(T element) {
        // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
        if (!cancelled) {
            if (whenNext(element)) {
                // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
                subscription.request(1);
            } else {
                // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
                doCancel();
            }
        }
    }

    private void doError(Throwable throwable) {
        // By rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
        cancelled = true;
        whenError(throwable);
    }

    private void doComplete() {
        // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
        cancelled = true;
        whenComplete();
    }

    private void doCancel() {
        cancelled = true;
        subscription.cancel();
    }

    // These classes represent the asynchronous signals.
    private interface Signal extends Runnable {
    }

    private class OnSubscribe implements Signal {
        private final Flow.Subscription subscription;

        OnSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void run() {
            doSubscribe(subscription);
        }
    }

    private class OnNext implements Signal {
        private final T element;

        OnNext(T element) {
            this.element = element;
        }

        @Override
        public void run() {
            doNext(element);
        }
    }

    private class OnError implements Signal {
        private final Throwable throwable;

        OnError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void run() {
            doError(throwable);
        }
    }

    private class OnComplete implements Signal {
        @Override
        public void run() {
            doComplete();
        }
    }

    // The non-blocking queue to transmit signals in a thread-safe way.
    private final Queue<Signal> signalsQueue = new ConcurrentLinkedQueue<>();

    // The mutex to establish the happens-before relationship between asynchronous signal calls.
    private final AtomicBoolean mutex = new AtomicBoolean(false);

    @Override
    public void run() {
        // By rule 2.7, a Subscriber must ensure that all calls on its Subscription's request, cancel methods are performed serially.
        if (mutex.get()) {
            try {
                Signal signal = signalsQueue.poll();
                logger.debug("({}) signal.poll {}", id, signal);
                if (!cancelled) {
                    signal.run();
                }
            } finally {
                mutex.set(false);
                if (!signalsQueue.isEmpty()) {
                    tryExecute();
                }
            }
        }
    }

    private void signal(Signal signal) {
        logger.debug("({}) signal.offer {}", id, signal);
        if (signalsQueue.offer(signal)) {
            tryExecute();
        }
    }

    private void tryExecute() {
        if (mutex.compareAndSet(false, true)) {
            try {
                executor.execute(this);
            } catch (Throwable throwable) {
                if (!cancelled) {
                    try {
                        doCancel();
                    } finally {
                        signalsQueue.clear();
                        mutex.set(false);
                    }
                }
            }
        }
    }
}