package demo.reactivestreams._part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class TckCompatibleSyncIteratorPublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(TckCompatibleSyncIteratorPublisher.class);

    private final Supplier<Iterator<? extends T>> iteratorSupplier;

    public TckCompatibleSyncIteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier) {
        this.iteratorSupplier = iteratorSupplier;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        IteratorSubscription subscription = new IteratorSubscription(subscriber);
        subscriber.onSubscribe(subscription);
        subscription.onSubscribed();
    }

    private class IteratorSubscription implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<? extends T> iterator;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicBoolean terminated = new AtomicBoolean(false);
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        IteratorSubscription(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            Iterator<? extends T> iterator = null;

            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable e) {
                error.set(e);
            }

            this.iterator = iterator;
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);

            if ((n < 1) && !terminated.getAndSet(true)) {
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
                } catch (Throwable e) {
                    if (!terminated.getAndSet(true)) {
                        subscriber.onError(e);
                    }
                }
            }

            if (!iterator.hasNext() && !terminated.getAndSet(true)) {
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            terminated.getAndSet(true);
        }

        void onSubscribed() {
            Throwable throwable = error.get();
            if ((throwable != null) && !terminated.getAndSet(true)) {
                subscriber.onError(throwable);
            }
        }
    }
}
