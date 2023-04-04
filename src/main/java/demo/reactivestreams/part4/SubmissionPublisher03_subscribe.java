package demo.reactivestreams.part4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher03_subscribe extends AbstractTest {

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            publisher.subscribe(new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    logger.info("subscribed: {}", subscription);
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(Long item) {
                    delay();
                    logger.info("next: {}", item);
                    this.subscription.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.error("error", throwable);
                }

                @Override
                public void onComplete() {
                    logger.info("completed");
                    countDownLatch.countDown();
                }
            });

            LongStream.range(0, 10).forEach(item -> {
                logger.info("submitted: {}", item);
                publisher.submit(item);
            });
            publisher.close();

            logger.info("wait...");
            countDownLatch.await();
            logger.info("finished");
        }
    }
}
