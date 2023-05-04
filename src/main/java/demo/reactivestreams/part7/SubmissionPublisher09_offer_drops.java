package demo.reactivestreams.part7;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher09_offer_drops extends AbstractTest {

    public static void main(String[] args) {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 4)) {

            List<Long> consumedItems = new ArrayList<>();
            List<Long> droppedItems = new ArrayList<>();

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed: {}", item);
                consumedItems.add(item);
            });

            LongStream.range(0, 10).forEach(item -> {
                    logger.info("offered: {}", item);
                    publisher.offer(item, (subscriber, value) -> {
                        delay();
                        logger.info("dropped: {}", value);
                        droppedItems.add(value);
                        return false;
                    });
                }
            );

            //delay(10);
            publisher.close();

            logger.info("wait...");
            while (!consumerFuture.isDone()) {
                delay();
            }
            logger.info("finished");

            logger.info("consumed: {}", consumedItems);
            logger.info("dropped: {}", droppedItems);
        }
    }
}