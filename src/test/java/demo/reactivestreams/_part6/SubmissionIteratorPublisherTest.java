package demo.reactivestreams._part6;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

@Test
public class SubmissionIteratorPublisherTest extends FlowPublisherVerification<Integer> {

    public SubmissionIteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return new SubmissionPublisher<>();
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return null;
    }
}
