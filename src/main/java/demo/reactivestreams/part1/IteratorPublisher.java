package demo.reactivestreams.part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

            if (n <= 0 && !terminate()) {
                subscriber.onError(new IllegalArgumentException("negative subscription request"));
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

            for (; demand.get() > 0 && iterator.hasNext() && !isTerminated(); demand.decrementAndGet()) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable e) {
                    if (!terminate()) {
                        subscriber.onError(e);
                    }
                }
            }

            if (!iterator.hasNext() && !terminate()) {
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            terminate();
        }

        void doOnSubscribed() {
            Throwable throwable = error.get();
            if (throwable != null && !terminate()) {
                subscriber.onError(throwable);
            }
        }

        private boolean terminate() {
            return isTerminated.getAndSet(true);
        }

        private boolean isTerminated() {
            return isTerminated.get();
        }
    }

    public static void main(String[] args) {
        new IteratorPublisher<>(() -> List.of(1, 2, 3, 4, 5, 6).iterator())
            .subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    System.out.println("Subscribed");
                    subscription.request(6);
                }

                @Override
                public void onNext(Integer item) {
                    System.out.println(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("Error: " + throwable);
                }

                @Override
                public void onComplete() {
                    System.out.println("Complete");
                }
            });
    }
}
