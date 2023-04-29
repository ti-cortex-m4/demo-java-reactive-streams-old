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
        // by_rule 1.11, a Publisher MAY support multiple Subscribers and decides whether each Subscription is unicast or multicast (unicast).
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        // by_rule 1.9, a Publisher MUST call onSubscribe prior onError if method subscribe fails.
        //subscriber.onSubscribe(subscription);
        //subscription.onSubscribed();
    }

    private class SubscriptionImpl implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final AtomicLong demand = new AtomicLong();
        //private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private Iterator<? extends T> iterator;

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            // by_rule 1.9, calling Publisher.subscribe must throw a java.lang.NullPointerException when the given parameter is null.
            this.subscriber = Objects.requireNonNull(subscriber);

            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable throwable) {
                // by_rule 1.9, a Publisher MUST call onSubscribe prior onError if method subscribe fails.
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void request(long n) {
                    }
                });
                // by_rule 1.4, if a Publisher fails it must signal an onError.
                doError(throwable);
            }

            if (!cancelled.get()) {
                subscriber.onSubscribe(this);
            }
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);

            // by_rule 3.9, while the Subscription is not cancelled, Subscription.request(long n) must signal onError with a java.lang.IllegalArgumentException if the argument is <= 0.
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

                // by_rule 3.8, while the Subscription is not cancelled, Subscription.request(long n) must register the given number of additional elements to be produced to the respective subscriber.
                long adjustedDemand = currentDemand + n;
                if (adjustedDemand < 0L) {
                    // by_rule 3.17, a Subscription must support a demand up to java.lang.Long.MAX_VALUE.
                    adjustedDemand = Long.MAX_VALUE;
                }

                // by_rule 3.3, Subscription.request MUST place an upper bound on possible synchronous recursion between Publisher and Subscriber.
                if (demand.compareAndSet(currentDemand, adjustedDemand)) {
                    if (currentDemand > 0) {
                        return;
                    }
                    break;
                }
            }

            // by_rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onError.
            for (; demand.get() > 0 && iterator.hasNext() && !cancelled.get(); demand.decrementAndGet()) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable throwable) {
                    if (!cancelled.get()) {
                        // by_rule 1.6, if a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
                        doCancel();
                        // by_rule 1.4, if a Publisher fails it must signal an onError.
                        subscriber.onError(throwable);
                    }
                }
            }

            // by_rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onComplete.
            if (!iterator.hasNext() && !cancelled.get()) {
                // by_rule 1.6, if a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
                doCancel();
                // by_rule 1.5, if a Publisher terminates successfully it must signal an onComplete.
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            doCancel();
        }
/*
        void onSubscribed() {
            Throwable throwable = error.get();
            if ((throwable != null) && !cancelled.get()) {
                doCancel();
                // by_rule 1.4, if a Publisher fails it must signal an onError.
                subscriber.onError(throwable);
            }
        }
*/
        private void doCancel() {
            logger.warn("subscription.terminate");
            cancelled.set(true);
        }

        private void doError(Throwable throwable) {
            // by_rule 1.6, if a Publisher signals either onError or onComplete on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
            cancelled.set(true);
            subscriber.onError(throwable);
        }
    }
}
