package part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher3_consume extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed: " + item);
            });

            LongStream.range(0, 10).forEach(item -> {
                logger.info("produced: " + item);
                publisher.submit(item);
            });
            publisher.close();

            consumerFuture.get();
        }
    }
}
