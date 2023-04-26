package demo.reactivestreams.demo4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

    private void doSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            // by rule 2.5, a `Subscriber` must cancel the given `Subscription` if it already has an active `Subscription`
            subscription.cancel();
        } else {
            this.subscription = subscription;
            this.subscription.request(1);
        }
    }

    private void doNext(T element) {
        if (!terminated) {
            if (whenNext(element)) {
                subscription.request(1);
            } else {
                doTerminate();
            }
        }
    }

    private void doError(Throwable throwable) {
        // by rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
        terminated = true;
        whenError(throwable);
    }

    private void doComplete() {
        // by rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
        terminated = true;
        whenComplete();
    }

    private final int id;
    private final Executor executor;
    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;
    private boolean terminated = false;

    public AsyncSubscriber(int id, Executor executor) {
        this.id = id;
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        // by rule 2.13, a `Subscriber` must throw a `java.lang.NullPointerException` if the `Subscription` is `null`
        signal(new OnSubscribe(Objects.requireNonNull(subscription)));
    }

    @Override
    public void onNext(T item) {
        logger.info("({}) subscriber.next: {}", id, item);
        // by rule 2.13, a `Subscriber` must throw a `java.lang.NullPointerException` if the `item` is `null`
        signal(new OnNext(Objects.requireNonNull(item)));
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
        // by rule 2.13, a `Subscriber` must throw a `java.lang.NullPointerException` if the `Throwable` is `null`
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

    protected boolean whenNext(T item) {
        return true;
    }

    protected void whenError(Throwable throwable) {
    }

    protected void whenComplete() {
        completed.countDown();
    }

    private void doTerminate() {
        logger.warn("({}) subscriber.terminate", id);
        terminated = true;
        subscription.cancel();
    }

    // `Signal` represents the asynchronous protocol between the `Publisher` and `Subscriber`
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

    // unbounded thread-safe queue for passing signals to be executed on other threads
    private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean mutex = new AtomicBoolean(false);

    @Override
    public void run() {
        if (mutex.get()) {
            try {
                Signal signal = inboundSignals.poll();
                logger.debug("({}) signal.poll {}", id, signal);
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

    private void signal(Signal signal) {
        logger.debug("({}) signal.offer {}", id, signal);
        if (inboundSignals.offer(signal)) {
            tryExecute();
        }
    }

    private void tryExecute() {
        if (mutex.compareAndSet(false, true)) {
            try {
                executor.execute(this);
            } catch (Throwable throwable) {
                if (!terminated) {
                    try {
                        doTerminate();
                    } finally {
                        inboundSignals.clear();
                        mutex.set(false);
                    }
                }
            }
        }
    }
}
