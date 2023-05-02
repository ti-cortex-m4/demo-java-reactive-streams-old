package demo.reactivestreams.part6;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher12_chaining_consume extends AbstractTest {

    public static void main(String[] args) {
        try (SubmissionPublisher<Long> publisher1 = new SubmissionPublisher<>();
             SubmissionPublisher<Long> publisher2 = new SubmissionPublisher<>();
             SubmissionPublisher<Long> publisher3 = new SubmissionPublisher<>()) {

            CompletableFuture<Void> consumerFuture3 = publisher3.consume(item -> {
                delay();
                logger.info("step 3: {}", item);
            });

            CompletableFuture<Void> consumerFuture2 = publisher2.consume(item -> {
                delay();
                logger.info("step 2: {}", item);
                publisher3.submit(item * 10);
            });

            CompletableFuture<Void> consumerFuture1 = publisher1.consume(item -> {
                delay();
                logger.info("step 1: {}", item);
                publisher2.submit(item * 10);
            });

            LongStream.range(1, 3).forEach(item -> {
                logger.info("submitted: {}", item);
                publisher1.submit(item);
            });

            publisher1.close();

            logger.info("(1) wait...");
            while (!consumerFuture1.isDone()) {
                delay();
            }
            logger.info("(1) completed");

            publisher2.close();

            logger.info("(2) wait...");
            while (!consumerFuture2.isDone()) {
                delay();
            }
            logger.info("(2) completed");

            publisher3.close();

            logger.info("(3) wait...");
            while (!consumerFuture3.isDone()) {
                delay();
            }
            logger.info("(3) completed");

            logger.info("finished");
        }
    }
}
