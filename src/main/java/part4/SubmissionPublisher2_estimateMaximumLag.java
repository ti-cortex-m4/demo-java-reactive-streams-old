package part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

// Returns an estimate of the maximum number of items produced but not yet consumed among all current subscribers.
public class SubmissionPublisher2_estimateMaximumLag extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            publisher.subscribe(new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                    logger.info("subscribed: " + subscription);
                }

                @Override
                public void onNext(Long item) {
                    delay();
                    this.subscription.request(1);

                    logger.info("next: " + item);
                    logger.info("estimateMaximumLag: " + publisher.estimateMaximumLag());
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.info("error: " + throwable);
                }

                @Override
                public void onComplete() {
                    logger.info("completed");
                }
            });

            LongStream.range(0, 10).forEach(publisher::submit);
//            publisher.close();

            ExecutorService executorService = (ExecutorService) publisher.getExecutor();
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        }

    }
}
