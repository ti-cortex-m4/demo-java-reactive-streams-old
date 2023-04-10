package demo.reactivestreams.part0;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Test
public class StreamPublisherTest extends FlowPublisherVerification<Integer> {

    public StreamPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return new StreamPublisher<>(() -> Stream.iterate(0, UnaryOperator.identity()).limit(elements));
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return new StreamPublisher<Integer>(() -> {
            throw new RuntimeException();
        });
    }
}
