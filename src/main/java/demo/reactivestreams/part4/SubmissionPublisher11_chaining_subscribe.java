package demo.reactivestreams.part4;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher11_chaining_subscribe extends AbstractTest {

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            Processor processor1 = new Processor("(1)");
            Processor processor2 = new Processor("(2)");
            Processor processor3 = new Processor("(3)");

            processor2.subscribe(processor3);
            processor1.subscribe(processor2);
            publisher.subscribe(processor1);

            LongStream.range(1, 3).forEach(item -> {
                logger.info("submitted: {}", item);
                publisher.submit(item);
            });
            publisher.close();

            logger.info("finished");

            ExecutorService executorService = (ExecutorService) publisher.getExecutor();
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    static class Processor extends SubmissionPublisher<Long> implements Flow.Processor<Long, Long> {

        private final String name;

        private Flow.Subscription subscription;

        Processor(String name) {
            this.name = name;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            logger.info("{} subscribed", name);
            this.subscription = subscription;
            this.subscription.request(1);
        }

        @Override
        public void onNext(Long item) {
            delay();
            logger.info("{} next: {}", name, item);
            submit(item * 10);
            this.subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("{} error: {}", name, throwable);
            closeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("{} completed", name);
            close();
        }
    }
}
