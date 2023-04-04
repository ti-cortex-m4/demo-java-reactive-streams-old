package demo.reactivestreams.part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisher02_constructor extends AbstractTest {

    public static void main(String[] args) {
        Executor executor = Executors.newSingleThreadExecutor();
        int maxBufferCapacity = 2;
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(executor, maxBufferCapacity)) {
            logger.info("executor: {}", publisher.getExecutor());
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());
        }
    }
}
