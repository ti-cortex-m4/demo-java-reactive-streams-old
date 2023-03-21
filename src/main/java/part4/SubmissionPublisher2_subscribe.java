package part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher2_subscribe extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

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
                    logger.info("received: {}", item);
                    this.subscription.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.error("error", throwable);
                }

                @Override
                public void onComplete() {
                    logger.info("completed");
                }
            });

            LongStream.range(0, 10).forEach(item -> {
                logger.info("produced: " + item);
                publisher.submit(item);
            });
            publisher.close();

            ExecutorService executorService = (ExecutorService) publisher.getExecutor();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
