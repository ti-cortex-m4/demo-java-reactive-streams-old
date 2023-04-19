package demo.reactivestreams.part0;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Test
public class SimpleIteratorPublisherTest extends FlowPublisherVerification<Integer> {

    public SimpleIteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        Iterator<Integer> iterator = Stream
            .iterate(0, UnaryOperator.identity())
            .limit(elements)
            .toList()
            .iterator();
        return new SimpleIteratorPublisher<>(() -> iterator);
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return new SimpleIteratorPublisher<>(() -> {
            throw new RuntimeException();
        });
    }
}
