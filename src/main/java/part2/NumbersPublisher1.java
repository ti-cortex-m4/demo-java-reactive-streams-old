package part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class NumbersPublisher1 extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersPublisher1.class);

    private final int count;

    NumbersPublisher1(int count) {
        this.count = count;
    }

    public static void main(String[] args) {
        NumbersPublisher1 publisher = new NumbersPublisher1(10);

        NumbersSubscriber1 subscriber = new NumbersSubscriber1();
        publisher.subscribe(subscriber);

        IntStream.range(0, publisher.count)
            .forEach(i -> {
                logger.info("Publisher.submit: {}", i);
                publisher.submit(i);
            });

        logger.info("Publisher.close");
        publisher.close();

        ForkJoinPool forkJoinPool = (ForkJoinPool) publisher.getExecutor();
        forkJoinPool.shutdown();
        try {
            forkJoinPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
