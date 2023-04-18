package demo.reactivestreams.part2;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Test
public class SubmissionIteratorPublisherTest extends FlowPublisherVerification<Integer> {

    public SubmissionIteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        Iterator<Integer> iterator = Stream
            .iterate(0, UnaryOperator.identity())
            .limit(elements)
            .toList()
            .iterator();
        return new SubmissionIteratorPublisher(iterator);
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return null;
    }
}
