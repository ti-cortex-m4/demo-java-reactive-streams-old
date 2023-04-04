package demo.reactivestreams.part4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

// returns an estimate of the minimum number of items requested (via request) but not yet produced, among all current subscribers.
public class SubmissionPublisher06_estimateMinimumDemand extends AbstractTest {

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            final int count = 10;
            CountDownLatch countDownLatch = new CountDownLatch(count);

            publisher.subscribe(new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    logger.info("subscribed");
                    this.subscription = subscription;
                    this.subscription.request(count);
                }

                @Override
                public void onNext(Long item) {
                    logger.info("next: {}", item);
                    logger.info("estimateMinimumDemand: {}", publisher.estimateMinimumDemand());

                    countDownLatch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.info("error", throwable);
                }

                @Override
                public void onComplete() {
                    logger.info("completed");
                }
            });

            LongStream.range(0, count).forEach(item -> {
                    delay();
                    logger.info("submitted: {}", item);
                    publisher.submit(item);
                }
            );

            logger.info("wait...");
            countDownLatch.await();
            logger.info("finished");
        }
    }
}
