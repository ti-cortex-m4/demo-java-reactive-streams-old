package demo.reactivestreams.demo1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SyncIteratorPublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(SyncIteratorPublisher.class);

    private final Supplier<Iterator<? extends T>> iteratorSupplier;

    public SyncIteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier) {
        this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        subscriber.onSubscribe(subscription);
        subscription.onSubscribed();
    }

    private class SubscriptionImpl implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<? extends T> iterator;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = Objects.requireNonNull(subscriber);

            Iterator<? extends T> iterator = null;
            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable throwable) {
                error.set(throwable);
            }
            this.iterator = iterator;
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);

            if ((n < 1) && !terminated.get()) {
                doTerminate();
                subscriber.onError(new IllegalArgumentException("non-positive subscription request"));
                return;
            }

            for (; ; ) {
                long currentDemand = demand.getAcquire();
                if (currentDemand == Long.MAX_VALUE) {
                    return;
                }

                long adjustedDemand = currentDemand + n;
                if (adjustedDemand < 0L) {
                    adjustedDemand = Long.MAX_VALUE;
                }

                if (demand.compareAndSet(currentDemand, adjustedDemand)) {
                    if (currentDemand > 0) {
                        return;
                    }
                    break;
                }
            }

            for (; demand.get() > 0 && iterator.hasNext() && !terminated.get(); demand.decrementAndGet()) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable throwable) {
                    if (!terminated.get()) {
                        doTerminate();
                        subscriber.onError(throwable);
                    }
                }
            }

            if (!iterator.hasNext() && !terminated.get()) {
                doTerminate();
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            doTerminate();
        }

        void onSubscribed() {
            Throwable throwable = error.get();
            if ((throwable != null) && !terminated.get()) {
                doTerminate();
                subscriber.onError(throwable);
            }
        }

        private void doTerminate() {
            logger.warn("subscription.terminate");
            terminated.set(true);
        }
    }
}
