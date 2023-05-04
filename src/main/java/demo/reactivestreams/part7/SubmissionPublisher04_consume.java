package demo.reactivestreams.part7;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher04_consume extends AbstractTest {

    public static void main(String[] args) {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed: {}", item);
            });

            LongStream.range(0, 10).forEach(item -> {
                logger.info("submitted: {}", item);
                publisher.submit(item);
            });
            publisher.close();

            logger.info("wait...");
            while (!consumerFuture.isDone()) {
                delay();
            }
            logger.info("finished");
        }
    }
}
