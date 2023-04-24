package demo.reactivestreams.demo2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

    private void doSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
        } else {
            this.subscription = subscription;
            this.subscription.request(1);
        }
    }

    private void doNext(T element) {
        if (!terminated.get()) {
            if (whenNext(element)) {
                subscription.request(1);
            } else {
                doTerminate();
            }
        }
    }

    private void doError(Throwable throwable) {
        terminated.set(true);
        whenError(throwable);
    }

    private void doComplete() {
        terminated.set(true);
        whenComplete();
    }

    private final int id;
    private final Executor executor;
    private final ExecutorImpl executorImpl;
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;

    public AsyncSubscriber(int id, Executor executor) {
        this.id = id;
        this.executor = Objects.requireNonNull(executor);
        this.executorImpl = new ExecutorImpl();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        executorImpl.signal(new OnSubscribe(Objects.requireNonNull(subscription)));
    }

    @Override
    public void onNext(T element) {
        logger.info("({}) subscriber.next: {}", id, element);
        executorImpl.signal(new OnNext(Objects.requireNonNull(element)));
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
        executorImpl.signal(new OnError(Objects.requireNonNull(throwable)));
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);
        executorImpl.signal(new OnComplete());
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    protected boolean whenNext(T element) {
        return true;
    }

    protected void whenError(Throwable throwable) {
    }

    protected void whenComplete() {
        completed.countDown();
    }

    private void doTerminate() {
        logger.warn("({}) subscriber.terminate", id);
        terminated.set(true);
        subscription.cancel();
    }

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

    private class ExecutorImpl implements Runnable {

        private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean mutex = new AtomicBoolean(false);

        @Override
        public void run() {
            if (mutex.get()) {
                try {
                    Signal signal = inboundSignals.poll();
                    logger.debug("({}) signal.poll {}", id, signal);
                    if (!terminated.get()) {
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
                    if (!terminated.get()) {
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
}
