package demo.reactivestreams.part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisher11_chaining_subscribe extends AbstractTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Integer> publisher1 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher2 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher3 = new SubmissionPublisher<>()) {

            CompletableFuture<Void> consumerFuture3 = publisher3.consume(item -> {
                delay();
                logger.info("step 3: {}", item);
            });

            CompletableFuture<Void> consumerFuture2 = publisher2.consume(item -> {
                delay();
                logger.info("step 2: {}", item);
                publisher3.submit(item * item);
            });

            CompletableFuture<Void> consumerFuture1 = publisher1.consume(item -> {
                delay();
                logger.info("step 1: {}", item);
                publisher2.submit(item * item);
            });

            publisher1.submit(2);
            publisher1.submit(3);
            publisher1.submit(5);

            publisher1.close();
            while (!consumerFuture1.isDone()) {
                logger.info("step 1: wait...");
                delay();
            }
            logger.info("step 1: completed");

            publisher2.close();
            while (!consumerFuture2.isDone()) {
                logger.info("step 2: wait...");
                delay();
            }
            logger.info("step 2: completed");

            publisher3.close();
            while (!consumerFuture3.isDone()) {
                logger.info("step 3: wait...");
                delay();
            }
            logger.info("step 3: completed");

            logger.info("finished");
        }
    }

    static class Step1 extends SubmissionPublisher<Long> implements Flow.Processor<Long, Long> {

        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            logger.info("step1.onSubscribe: {}", subscription);
            this.subscription = subscription;
            this.subscription.request(1);

        }

        @Override
        public void onNext(Long item) {
            logger.info("step1.onNext: {}", item);
            submit(item);
            this.subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("step1.onError", throwable);
            closeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("step1.onComplete");
            close();
        }
    }

}
