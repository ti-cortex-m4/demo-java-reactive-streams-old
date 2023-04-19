package demo.reactivestreams.part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class IteratorPublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(IteratorPublisher.class);

    private final Supplier<Iterator<? extends T>> iteratorSupplier;

    public IteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier) {
        this.iteratorSupplier = iteratorSupplier;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        IteratorSubscription subscription = new IteratorSubscription(subscriber);
        subscriber.onSubscribe(subscription);
        subscription.doOnSubscribed();
    }

    private class IteratorSubscription implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<? extends T> iterator;
        private final AtomicBoolean isTerminated = new AtomicBoolean(false);
        private final AtomicLong demand = new AtomicLong();
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

            if (n <= 0) {
                if (!isTerminated.getAndSet(true)) {
                    subscriber.onError(new IllegalArgumentException("negative subscription request"));
                    return;
                }
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

            for (; demand.get() > 0 && iterator.hasNext() && !isTerminated.get(); demand.decrementAndGet()) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable e) {
                    if (!isTerminated.getAndSet(true)) {
                        subscriber.onError(e);
                    }
                }
            }

            if (!iterator.hasNext()) {
                if (!isTerminated.getAndSet(true)) {
                    subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            isTerminated.getAndSet(true);
        }

        void doOnSubscribed() {
            Throwable throwable = error.get();
            if (throwable != null) {
                if (!isTerminated.getAndSet(true)) {
                    subscriber.onError(throwable);
                }
            }
        }

    }
}
