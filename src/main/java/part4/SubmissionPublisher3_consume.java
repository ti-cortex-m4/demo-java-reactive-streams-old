package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher3_consume extends SomeTest{

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            CompletableFuture<Void> consumerFuture = publisher
                .consume(item -> {
                    delay();
                    logger.info("consumed: " + item);
                });

            LongStream.range(0, 10).forEach(item -> {
                delay();
                logger.info("produced: " + item);
                publisher.submit(item);
            });

            ExecutorService executorService = (ExecutorService)publisher.getExecutor();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            publisher.close();

            consumerFuture.get();
        }
    }
}
