package demo.reactivestreams.part00;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class SimpleIteratorPublisherTest extends FlowPublisherVerification<Integer> {

    public SimpleIteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return new SimpleIteratorPublisher((int) elements);
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return null;
    }
}
