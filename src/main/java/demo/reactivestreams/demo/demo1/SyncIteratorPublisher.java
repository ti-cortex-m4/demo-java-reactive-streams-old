package demo.reactivestreams.demo.demo1;

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
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            // by rule 1.9, a `Publisher.subscribe` must throw a `java.lang.NullPointerException` if the `Subscriber` is `null`
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

            // by rule 3.9, While the Subscription is not cancelled, Subscription.request(long n) MUST signal onError with a java.lang.IllegalArgumentException if the argument is <= 0.
            if ((n <= 0) && !cancelled.get()) {
                doCancel();
                subscriber.onError(new IllegalArgumentException("non-positive subscription request"));
                return;
            }

            for (; ; ) {
                long currentDemand = demand.getAcquire();
                if (currentDemand == Long.MAX_VALUE) {
                    return;
                }

                // by rule 3.8, While the Subscription is not cancelled, Subscription.request(long n) MUST register the given number of additional elements to be produced to the respective subscriber.
                long adjustedDemand = currentDemand + n;
                if (adjustedDemand < 0L) {
                    // by rule 3.17, a `Subscription` must support a demand up to `java.lang.Long.MAX_VALUE`
                    adjustedDemand = Long.MAX_VALUE;
                }

                if (demand.compareAndSet(currentDemand, adjustedDemand)) {
                    if (currentDemand > 0) {
                        return;
                    }
                    break;
                }
            }

            for (; demand.get() > 0 && iterator.hasNext() && !cancelled.get(); demand.decrementAndGet()) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable throwable) {
                    if (!cancelled.get()) {
                        // by rule 1.6, If a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription MUST be considered cancelled.
                        doCancel();
                        // by rule 1.4, if a Publisher fails it must signal an onError.
                        subscriber.onError(throwable);
                    }
                }
            }

            if (!iterator.hasNext() && !cancelled.get()) {
                // by rule 1.6, If a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription MUST be considered cancelled.
                doCancel();
                // by rule 1.5, If a Publisher terminates successfully it MUST signal an onComplete.
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            doCancel();
        }

        void onSubscribed() {
            Throwable throwable = error.get();
            if ((throwable != null) && !cancelled.get()) {
                doCancel();
                // by rule 1.4, if a Publisher fails it must signal an onError.
                subscriber.onError(throwable);
            }
        }

        private void doCancel() {
            logger.warn("subscription.terminate");
            cancelled.set(true);
        }
    }
}