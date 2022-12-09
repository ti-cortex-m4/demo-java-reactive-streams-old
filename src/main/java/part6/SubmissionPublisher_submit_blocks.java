package part6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import part2.NumbersProcessor2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher_submit_blocks {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPublisher_submit_blocks.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 2)) {
            System.out.println("getExecutor: " + publisher.getExecutor());
            System.out.println("getMaxBufferCapacity: " + publisher.getMaxBufferCapacity());

            CompletableFuture<Void> future = publisher.consume(item -> {
                logger.info("before consume: " + item);
                delay();
                logger.info("after consume:  " + item);
            });
            System.out.println("getNumberOfSubscribers: " + publisher.getNumberOfSubscribers());

            LongStream.range(0, 10).forEach(item -> {
                    logger.info("before submit: " + item);
                    publisher.submit(item);
                    logger.info("after submit:  " + item);
                }
            );
            future.get();
        }
    }

    private static void delay() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
