package part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class NumbersPublisher2 extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersPublisher2.class);

    private final int count;

    NumbersPublisher2(int count) {
        this.count = count;
    }

    public static void main(String[] args) {
        NumbersPublisher2 publisher = new NumbersPublisher2(10);

        NumbersProcessor2 processor = new NumbersProcessor2();
        publisher.subscribe(processor);

        NumbersSubscriber2 subscriber = new NumbersSubscriber2();
        processor.subscribe(subscriber);

        IntStream.range(0, publisher.count)
            .forEach(i -> {
                logger.info("Publisher.submit: {}", i);
                publisher.submit(i);
            });

        logger.info("Publisher.close");
        publisher.close();

        ForkJoinPool forkJoinPool = (ForkJoinPool) publisher.getExecutor();
        forkJoinPool.shutdown();
        try {
            forkJoinPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/ExecutorService.html
    protected static void shutdown(ExecutorService executorService) {
        logger.debug("executor service: shutdown started");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                List<Runnable> skippedTasks = executorService.shutdownNow();
                logger.error("count of tasks never commenced execution: {}", skippedTasks.size());
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.error("executor service didn't terminate");
                }
            }
        } catch (InterruptedException e) {
            logger.error("executor service is interrupted", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.debug("executor service: shutdown finished");
    }
}
