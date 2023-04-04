package demo.reactivestreams.part4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

// Returns an estimate of the maximum number of items produced but not yet consumed among all current subscribers.
public class SubmissionPublisher07_estimateMaximumLag extends AbstractTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            publisher.subscribe(new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    logger.info("subscribed");
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(Long item) {
                    delay();
                    this.subscription.request(1);

                    logger.info("next: {}", item);
                    logger.info("estimateMaximumLag: {}", publisher.estimateMaximumLag());
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.info("error", throwable);
                }

                @Override
                public void onComplete() {
                    logger.info("completed");
                    countDownLatch.countDown();
                }
            });

            LongStream.range(0, 3).forEach(item -> {
                    logger.info("submitted: {}", item);
                    publisher.submit(item);
                }
            );
//            publisher.close();

            logger.info("wait...");
            countDownLatch.await();
            logger.info("finished");
        }

    }
}
