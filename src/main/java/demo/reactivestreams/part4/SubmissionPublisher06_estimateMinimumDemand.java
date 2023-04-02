package demo.reactivestreams.part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

// Returns an estimate of the minimum number of items requested (via request) but not yet produced, among all current subscribers.
public class SubmissionPublisher06_estimateMinimumDemand extends AbstractTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            publisher.subscribe(new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    logger.info("subscribed");
                    this.subscription = subscription;
                    this.subscription.request(10);
                }

                @Override
                public void onNext(Long item) {
                    logger.info("next: {}", item);
                    logger.info("estimateMinimumDemand: {}", publisher.estimateMinimumDemand());
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

            LongStream.range(0, 10).forEach(item -> {
                    delay();
                    publisher.submit(item);
                }
            );
            publisher.close();

            ExecutorService executorService = (ExecutorService) publisher.getExecutor();
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        }
    }
}
