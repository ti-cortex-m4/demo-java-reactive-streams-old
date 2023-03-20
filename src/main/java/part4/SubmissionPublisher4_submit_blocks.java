package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher4_submit_blocks extends SomeTest {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPublisher4_submit_blocks.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 2)) {
            System.out.println("getMaxBufferCapacity: " + publisher.getMaxBufferCapacity());

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
                delay();
                logger.info("consumed:  " + item);
            });

            LongStream.range(0, 10).forEach(item -> {
                    publisher.submit(item);
                    logger.info("submitted: " + item);
                }
            );

            ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
            publisher.close();

            consumerFuture.get();
        }
    }
}
