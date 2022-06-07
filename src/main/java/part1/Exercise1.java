package part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Stream;

public class Exercise1 {

    private static final Logger logger = LoggerFactory.getLogger(Exercise1.class);

    public static void main(String[] args) {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

        LipsumSubscriber lipsumSubscriber = new LipsumSubscriber();

        publisher.subscribe(lipsumSubscriber);

        String filePath = "res/lipsum.txt";

        try (Stream<String> words = Files.lines(Paths.get(filePath))) {
            words.flatMap((l) -> Arrays.stream(l.split("[\\s.,\\n]+"))).forEach(publisher::submit);
            publisher.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
