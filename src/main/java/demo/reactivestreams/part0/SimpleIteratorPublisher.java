package demo.reactivestreams.part0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class SimpleIteratorPublisher implements Flow.Publisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleIteratorPublisher.class);

    private final Iterator<Integer> iterator;

    public SimpleIteratorPublisher(int count) {
        this.iterator = IntStream.rangeClosed(1, count).iterator();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        logger.info("publisher.subscribe");
        subscriber.onSubscribe(new SimpleSubscription(subscriber));

        try {
            iterator.forEachRemaining(item -> {
                logger.info("publisher.next: {}", item);
                subscriber.onNext(item);
            });

            logger.info("publisher.complete");
            subscriber.onComplete();
        } catch (Throwable t) {
            logger.info("publisher.error");
            subscriber.onError(t);
        }
    }

    private class SimpleSubscription implements Flow.Subscription {

        private final Flow.Subscriber<? super Integer> subscriber;
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        public SimpleSubscription(Flow.Subscriber<? super Integer> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            logger.info("subscription.request: {}", n);

            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
            }

            for (long i = n; i > 0 && iterator.hasNext() && !terminated.get(); i--) {
                subscriber.onNext(iterator.next());
            }

            if (!iterator.hasNext() && !terminated.getAndSet(true)) {
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            logger.info("subscription.cancel");
            terminated.set(true);
        }
    }

    public static void main(String[] args) {
        new SimpleIteratorPublisher(10).subscribe(new NoBackpressureSubscriber());
    }
}
