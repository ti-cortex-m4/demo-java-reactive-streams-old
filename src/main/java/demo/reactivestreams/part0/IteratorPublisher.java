package demo.reactivestreams.part0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
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
    }

    private class IteratorSubscription implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<? extends T> iterator;
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        IteratorSubscription(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            this.iterator = iteratorSupplier.get();
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);

            if ((n <= 0) && !terminated.getAndSet(true)) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }

            for (long demand = n; demand > 0 && iterator.hasNext() && !terminated.get(); demand--) {
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
    }
}
