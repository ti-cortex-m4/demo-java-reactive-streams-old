package demo.reactivestreams.part1b;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

@Test
public class RangePublisherTest extends PublisherVerification<Integer> {
    public RangePublisherTest() {
        super(new TestEnvironment(50, 50));
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return new RangePublisher(1, (int)elements);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }
}
