package demo.reactivestreams.part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class SubmissionPublisher11_chaining extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Integer> publisher1 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher2 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher3 = new SubmissionPublisher<>()) {

            publisher3.consume(item -> {
                delay();
                logger.info("step 3: {}", item);
            });

            publisher2.consume(item -> {
                delay();
                logger.info("step 2: {}", item);
                publisher3.submit(item * item);
            });

            publisher1.consume(item -> {
                delay();
                logger.info("step 1: {}", item);
                publisher2.submit(item * item);
            });

            publisher1.submit(2);
            publisher1.submit(3);
            publisher1.submit(5);
            publisher1.submit(7);

            ForkJoinPool.commonPool().awaitTermination(60, TimeUnit.SECONDS);
        }
    }
}
