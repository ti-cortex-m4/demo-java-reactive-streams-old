package demo.reactivestreams.part4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher08_submit_blocks extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 3)) {
            logger.info("getMaxBufferCapacity: {}", publisher.getMaxBufferCapacity());

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

            while (!consumerFuture.isDone()) {
                logger.info("wait...");
                delay();
            }
            logger.info("completed");

            logger.info("consumed: {}", consumedItems);
        }
    }
}
