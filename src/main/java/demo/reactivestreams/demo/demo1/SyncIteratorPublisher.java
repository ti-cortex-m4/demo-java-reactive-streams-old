package demo.reactivestreams.demo.demo1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SyncIteratorPublisher<T> implements Flow.Publisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(SyncIteratorPublisher.class);

    private final Supplier<Iterator<? extends T>> iteratorSupplier;

    public SyncIteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier) {
        this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        // By rule 1.11, a Publisher may support multiple Subscribers and decides whether each Subscription is unicast or multicast.
        new SubscriptionImpl(subscriber);
    }

    private class SubscriptionImpl implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private Iterator<? extends T> iterator;

        SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
            // By rule 1.9, calling Publisher.subscribe must throw a NullPointerException when the given parameter is null.
            this.subscriber = Objects.requireNonNull(subscriber);

            try {
                iterator = iteratorSupplier.get();
            } catch (Throwable throwable) {
                // By rule 1.9, a Publisher must call onSubscribe prior onError if Publisher.subscribe(Subscriber subscriber) fails.
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void request(long n) {
                    }
                });
                // By rule 1.4, if a Publisher fails it must signal an onError.
                doError(throwable);
            }

            if (!cancelled.get()) {
                subscriber.onSubscribe(this);
            }
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);

            // By rule 3.9, while the Subscription is not cancelled, Subscription.request(long n) must signal onError with a IllegalArgumentException if the argument is <= 0.
            if ((n <= 0) && !cancelled.get()) {
                doCancel();
                subscriber.onError(new IllegalArgumentException("non-positive subscription request"));
                return;
            }

            for (;;) {
                long currentDemand = demand.getAcquire();
                if (currentDemand == Long.MAX_VALUE) {
                    return;
                }

                // By_rule 3.8, while the Subscription is not cancelled, Subscription.request(long n) must register the given number of additional elements to be produced to the respective subscriber.
                long adjustedDemand = currentDemand + n;
                if (adjustedDemand < 0L) {
                    // By rule 3.17, a Subscription must support a demand up to Long.MAX_VALUE.
                    adjustedDemand = Long.MAX_VALUE;
                }

                // By rule 3.3, Subscription.request must place an upper bound on possible synchronous recursion between Publisher and Subscriber.
                if (demand.compareAndSet(currentDemand, adjustedDemand)) {
                    if (currentDemand > 0) {
                        return;
                    }
                    break;
                }
            }

            // By rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onError.
            for (; demand.get() > 0 && iterator.hasNext() && !cancelled.get(); demand.decrementAndGet()) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable throwable) {
                    if (!cancelled.get()) {
                        // By rule 1.6, if a Publisher signals onError on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
                        doCancel();
                        // By rule 1.4, if a Publisher fails it must signal an onError.
                        subscriber.onError(throwable);
                    }
                }
            }

            // By rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onComplete.
            if (!iterator.hasNext() && !cancelled.get()) {
                // By rule 1.6, if a Publisher signals onComplete on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
                doCancel();
                // By rule 1.5, if a Publisher terminates successfully it must signal an onComplete.
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            doCancel();
        }

        private void doCancel() {
            logger.info("subscription.cancelled");
            cancelled.set(true);
        }

        private void doError(Throwable throwable) {
            // By rule 1.6, if a Publisher signals onError on a Subscriber, that Subscriber’s Subscription must be considered cancelled.
            cancelled.set(true);
            subscriber.onError(throwable);
        }
    }
}
