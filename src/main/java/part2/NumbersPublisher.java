package part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SubmissionPublisher;

public class NumbersPublisher extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersPublisher.class);

    private final int count;

    NumbersPublisher(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
