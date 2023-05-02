package demo.reactivestreams.part6;

import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisher01_no_arg_constructor extends AbstractTest {

    public static void main(String[] args) {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            logger.info("executor: {}", publisher.getExecutor());
            logger.info("maximum buffer capacity: {}", publisher.getMaxBufferCapacity());
        }
    }
}
