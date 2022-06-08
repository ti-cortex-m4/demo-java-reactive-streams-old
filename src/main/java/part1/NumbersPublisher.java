package part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NumbersPublisher extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersPublisher.class);

    private final int count;

    NumbersPublisher(int count) {
        this.count = count;
    }

    public static void main(String[] args) {
        NumbersPublisher publisher = new NumbersPublisher(10);

        NumbersSubscriber subscriber = new NumbersSubscriber();
        publisher.subscribe(subscriber);

        IntStream.range(0, publisher.count)
            .forEach(i -> {
                logger.info("Publish: {}", i);
                publisher.submit(i);
            });

        publisher.close();
    }
}
