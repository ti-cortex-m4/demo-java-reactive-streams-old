package part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher1_pool extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            logger.info("executor: {}", publisher.getExecutor());
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());

            CompletableFuture<Void> consumerFuture1 = publisher.consume(item -> {
                delay();
                logger.info("consumed by consumer 1: {}", item);
            });

            CompletableFuture<Void> consumerFuture2 = publisher.consume(item -> {
                delay();
                logger.info("consumed by consumer 2: {}", item);
            });
            logger.info("number of subscribers: {}", publisher.getNumberOfSubscribers());

            LongStream.range(0, 10).forEach(publisher::submit);
            publisher.close();

            delay(5);
            consumerFuture1.cancel(true);
            logger.info("number of subscribers: {}", publisher.getNumberOfSubscribers());

            while (!consumerFuture2.isDone()) {
                logger.info("wait...");
                delay();
            }
            logger.info("number of subscribers: {}", publisher.getNumberOfSubscribers());

            ExecutorService executorService = (ExecutorService) publisher.getExecutor();
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
