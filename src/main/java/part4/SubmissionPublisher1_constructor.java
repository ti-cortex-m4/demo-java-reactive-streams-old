
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

public class SubmissionPublisher1_constructor {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPublisher1_constructor.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int maxBufferCapacity = Flow.defaultBufferSize() ;

        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(executorService, maxBufferCapacity)) {
            System.out.println("getExecutor: " + publisher.getExecutor());
            System.out.println("getMaxBufferCapacity: " + publisher.getMaxBufferCapacity());

            CompletableFuture<Void> consumerFuture1 = publisher.consume(item -> logger.info("consumer1 : " + item));
            CompletableFuture<Void> consumerFuture2 = publisher.consume(item -> logger.info("consumer2 : " + item));
            System.out.println("getNumberOfSubscribers: " + publisher.getNumberOfSubscribers());

            LongStream.range(0, 10).forEach(publisher::submit);

            ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
            publisher.close();

            consumerFuture1.get();
            consumerFuture2.get();
        }
    }
}
