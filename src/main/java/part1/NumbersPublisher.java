package part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NumbersPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NumbersPublisher.class);

    public static void main(String[] args) {
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();

        NumbersSubscriber subscriber = new NumbersSubscriber();
        publisher.subscribe(subscriber);

        IntStream.range(0,10)
            .forEach( i -> {
                logger.info("Publish: {}", i);
                publisher.submit(i);
            });

        publisher.close();
    }
}
