package demo.reactivestreams.part1;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Test
public class TckCompatibleSyncIteratorPublisherTest extends FlowPublisherVerification<Integer> {

    public TckCompatibleSyncIteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return new TckCompatibleSyncIteratorPublisher<>(
            () -> Stream
                .iterate(0, UnaryOperator.identity())
                .limit(elements)
                .iterator()
        );
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return new TckCompatibleSyncIteratorPublisher<>(
            () -> {
                throw new RuntimeException();
            });
    }
}
