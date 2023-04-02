package demo.reactivestreams.part4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher10_offer_repeats extends AbstractTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 3)) {

            List<Long> consumedItems = new ArrayList<>();
            List<Long> repeatedItems = new ArrayList<>();

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed: {}", item);
                consumedItems.add(item);
            });

            LongStream.range(0, 10).forEach(item -> {
                    logger.info("offered: {}", item);
                    publisher.offer(item, (subscriber, value) -> {
                        delay();
                        logger.info("repeated: {}", value);
                        repeatedItems.add(value);
                        return true;
                    });
                }
            );

            publisher.close();

            while (!consumerFuture.isDone()) {
                logger.info("wait...");
                delay();
            }
            logger.info("completed");

            logger.info("consumed: {}", consumedItems);
            logger.info("repeated: {}", repeatedItems);
        }
    }
}
