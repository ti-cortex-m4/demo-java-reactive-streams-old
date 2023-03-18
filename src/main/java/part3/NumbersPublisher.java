package part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class NumbersPublisher extends SubmissionPublisher<Integer> {

    private final int count;

    public NumbersPublisher(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
