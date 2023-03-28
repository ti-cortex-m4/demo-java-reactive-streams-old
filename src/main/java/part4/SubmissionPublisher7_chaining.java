package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class SubmissionPublisher7_chaining {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPublisher7_chaining.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Integer> publisher1 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher2 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher3 = new SubmissionPublisher<>()) {

            publisher3.consume(x -> logger.info("step 3: {}", x));

            publisher2.consume(x -> {
                logger.info("step 2: {}", x);
                delay();
                publisher3.submit(x * x);
            });

            publisher1.consume(x -> {
                logger.info("step 1: {}", x);
                delay();
                publisher2.submit(x * x);
            });

            publisher1.submit(2);
            publisher1.submit(3);
            publisher1.submit(5);

            ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void delay() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
