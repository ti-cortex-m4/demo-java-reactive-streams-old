package demo.reactivestreams.part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisher1_constructor extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            logger.info("executor: {}", publisher.getExecutor());
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());
        }
    }
}
