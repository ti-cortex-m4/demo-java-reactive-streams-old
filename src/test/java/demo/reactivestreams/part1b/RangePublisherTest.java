package demo.reactivestreams.part1b;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class RangePublisherTest extends FlowPublisherVerification<Integer> {
    public RangePublisherTest() {
        super(new TestEnvironment(50, 50));
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return new RangePublisher(1, (int)elements);
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return null;
    }
}
