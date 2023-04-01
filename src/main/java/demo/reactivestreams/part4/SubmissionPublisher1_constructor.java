package demo.reactivestreams.part4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher1_constructor extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Executor executor = Executors.newSingleThreadExecutor();
        int maxBufferCapacity = 1;
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(executor, maxBufferCapacity)) {
            logger.info("executor: {}", publisher.getExecutor());
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());
        }
    }
}
