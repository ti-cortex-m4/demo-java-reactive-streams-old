package demo.reactivestreams.part1;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Test
public class IteratorPublisherTest extends FlowPublisherVerification<Integer> {

    public IteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return new IteratorPublisher<>(() -> {
            List<Integer> list = Stream
                .iterate(0, UnaryOperator.identity())
                .limit(elements)
                .toList();
            return list.iterator();
        });
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return new IteratorPublisher<Integer>(() -> {
            throw new RuntimeException();
        });
    }
}
