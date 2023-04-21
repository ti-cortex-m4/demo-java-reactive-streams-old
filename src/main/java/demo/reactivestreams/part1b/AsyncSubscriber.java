package demo.reactivestreams.part1b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

    private interface Signal extends Runnable {
    }

    private class OnSubscribe implements Signal {
        public final Flow.Subscription s;

        public OnSubscribe(Flow.Subscription s) {
            this.s = s;
        }

        @Override
        public void run() {
            if (subscription != null) {
                s.cancel();
            } else {
                subscription = s;
                subscription.request(1);
            }
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

    protected boolean whenNext(T element) {
        return true;
    }

    protected void whenError(Throwable throwable) {
    }

    protected void whenComplete() {
    }

    @Override
    public void onSubscribe(Flow.Subscription s) {
        logger.info("subscriber.subscribe: {}", s);
        if (s == null) {
            throw new NullPointerException();
        }

        signal(new OnSubscribe(s));
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
