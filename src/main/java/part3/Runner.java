package part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(NumbersPublisher.class);

    public static void main(String[] args) throws InterruptedException {
        NumbersPublisher publisher = new NumbersPublisher(10);

        NumbersFilterProcessor processor = new NumbersFilterProcessor();
        publisher.subscribe(processor);

        NumbersSubscriber subscriber = new NumbersSubscriber();
        processor.subscribe(subscriber);

        IntStream.range(0, publisher.getCount())
            .forEach(i -> {
                logger.info("Publisher.submit: {}", i);
                publisher.submit(i);
            });

        logger.info("Publisher.close");
        publisher.close();

        ForkJoinPool forkJoinPool = (ForkJoinPool) publisher.getExecutor();
        forkJoinPool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
