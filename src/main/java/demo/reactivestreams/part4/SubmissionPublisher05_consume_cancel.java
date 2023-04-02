package demo.reactivestreams.part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher05_consume_cancel extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            CompletableFuture<Void> consumerFuture1 = publisher.consume(item -> {
                delay();
                logger.info("consumed by consumer 1: {}", item);
            });

            CompletableFuture<Void> consumerFuture2 = publisher.consume(item -> {
                delay();
                logger.info("consumed by consumer 2: {}", item);
            });
            logger.info("number of subscribers: {}", publisher.getNumberOfSubscribers());

            LongStream.range(0, 10).forEach(item -> {
                logger.info("submitted: {}", item);
                publisher.submit(item);
            });
            publisher.close();

            delay(5);
            consumerFuture2.cancel(true);

            while (!consumerFuture1.isDone()) {
                logger.info("wait...");
                delay();
            }
            logger.info("completed");
        }
    }
}
