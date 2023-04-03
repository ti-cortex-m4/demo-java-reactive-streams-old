package demo.reactivestreams.part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisher11_chaining extends AbstractTest {

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
                logger.info("wait1...");
                delay();
            }
            logger.info("completed1");

            while (!consumerFuture2.isDone()) {
                logger.info("wait2...");
                delay();
            }
            logger.info("completed2");

            while (!consumerFuture3.isDone()) {
                logger.info("wait3...");
                delay();
            }
            logger.info("completed3");

//            CompletableFuture<Void> futures = CompletableFuture.allOf(consumerFuture1,consumerFuture2,consumerFuture3);
//            logger.info("wait...");
//
//            futures.get();
            logger.info("completed");

//            ForkJoinPool.commonPool().awaitTermination(60, TimeUnit.SECONDS);
        }
    }
}
