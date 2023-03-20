package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher1_pool {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPublisher1_pool.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        int maxBufferCapacity = Flow.defaultBufferSize() ;

        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(executorService, maxBufferCapacity)) {
            System.out.println("getExecutor: " + publisher.getExecutor());
            System.out.println("getMaxBufferCapacity: " + publisher.getMaxBufferCapacity());

            CompletableFuture<Void> consumerFuture1 = publisher.consume(item -> logger.info("consumed 1: " + item));
            CompletableFuture<Void> consumerFuture2 = publisher.consume(item -> logger.info("consumed 2: " + item));
            CompletableFuture<Void> consumerFuture3 = publisher.consume(item -> logger.info("consumed 3: " + item));

            LongStream.range(0, 10).forEach(publisher::submit);

            ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
            publisher.close();

            consumerFuture1.get();
            consumerFuture2.get();
            consumerFuture3.get();
        }
    }
}
