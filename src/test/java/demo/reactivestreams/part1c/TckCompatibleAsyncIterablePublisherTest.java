package demo.reactivestreams.part1c;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Test
public class TckCompatibleAsyncIterablePublisherTest extends FlowPublisherVerification<Integer> {

    private ExecutorService executorService;

    @BeforeClass
    void before() {
        executorService = Executors.newFixedThreadPool(4);
    }

    @AfterClass
    void after() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public TckCompatibleAsyncIterablePublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(final long elements) {
        assert (elements <= maxElementsFromPublisher());
        Iterator<Integer> iterator = Stream
            .iterate(0, UnaryOperator.identity())
            .limit(elements)
            .iterator();
        return new TckCompatibleAsyncIterablePublisher<>(() -> iterator, executorService);
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return new TckCompatibleAsyncIterablePublisher<Integer>(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new RuntimeException("Error state signal!");
            }
        }, executorService);
    }

    @Override
    public long maxElementsFromPublisher() {
        return Integer.MAX_VALUE;
    }
}
