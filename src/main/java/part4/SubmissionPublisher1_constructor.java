
package part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher1_constructor extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        int maxBufferCapacity = Flow.defaultBufferSize();

        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(executorService, maxBufferCapacity)) {
            logger.info("executor: {}", publisher.getExecutor());
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());

            CompletableFuture<Void> consumerFuture = publisher.consume(item -> logger.info("consumed by consumer 1: {}", item));
            logger.info("number of subscribers: {}", publisher.getNumberOfSubscribers());

            LongStream.range(0, 10).forEach(publisher::submit);

            ( (ExecutorService)publisher.getExecutor()).awaitTermination(10, TimeUnit.SECONDS);
            publisher.close();

            consumerFuture.get();
        }
    }
}
