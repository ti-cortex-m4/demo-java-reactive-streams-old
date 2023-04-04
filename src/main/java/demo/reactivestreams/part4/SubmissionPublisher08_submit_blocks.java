package demo.reactivestreams.part4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher08_submit_blocks extends AbstractTest {

    public static void main(String[] args) {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 4)) {
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());

            List<Long> consumedItems = new ArrayList<>();

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed: {}", item);
                consumedItems.add(item);
            });

            LongStream.range(0, 10).forEach(item -> {
                    logger.info("submitted: {}", item);
                    publisher.submit(item);
                }
            );
            publisher.close();

            logger.info("wait...");
            while (!consumerFuture.isDone()) {
                delay();
            }
            logger.info("finished");

            logger.info("consumed: {}", consumedItems);
        }
    }
}
