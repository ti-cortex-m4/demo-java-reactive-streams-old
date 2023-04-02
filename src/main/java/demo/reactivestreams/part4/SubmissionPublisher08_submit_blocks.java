package demo.reactivestreams.part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher08_submit_blocks extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 2)) {
            logger.info("getMaxBufferCapacity: {}", publisher.getMaxBufferCapacity());

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed:  {}", item);
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
//            ForkJoinPool.commonPool().awaitTermination(60, TimeUnit.SECONDS);
//            publisher.close();
//
//            consumerFuture.get();
        }
    }
}
