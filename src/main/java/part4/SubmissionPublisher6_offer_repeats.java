package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher6_offer_repeats extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 2)) {

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed: " + item);
            });

            LongStream.range(0, 10).forEach(item -> {
                logger.info("offered: " + item);
                    publisher.offer(item, (subscriber, value) -> {
                        delay();
                        logger.info("repeated: " + value);
                        return true;
                    });
                }
            );

            publisher.close();

            while (!consumerFuture.isDone()) {
                logger.info("wait...");
                delay();
            }
        }
    }
}
