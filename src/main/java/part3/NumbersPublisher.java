package part3;

import java.util.concurrent.SubmissionPublisher;

public class NumbersPublisher extends SubmissionPublisher<Integer> {

    private final int count;

    public NumbersPublisher(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
