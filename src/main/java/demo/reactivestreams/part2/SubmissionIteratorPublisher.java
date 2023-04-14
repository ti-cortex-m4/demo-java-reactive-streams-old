package demo.reactivestreams.part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class SubmissionIteratorPublisher extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionIteratorPublisher.class);

    private final Iterator<Integer> iterator;

    public SubmissionIteratorPublisher(int count) {
        this.iterator = IntStream.rangeClosed(1, count).iterator();
    }

    public Iterator<Integer> getIterator() {
        return iterator;
    }
}
