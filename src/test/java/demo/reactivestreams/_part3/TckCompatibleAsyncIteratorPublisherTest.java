package demo.reactivestreams._part3;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.assertTrue;

@Test
public class TckCompatibleAsyncIteratorPublisherTest extends FlowPublisherVerification<Integer> {

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

    public TckCompatibleAsyncIteratorPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        assertTrue(elements <= maxElementsFromPublisher());
        return new TckCompatibleAsyncIteratorPublisher<>(
            () -> Stream
                .iterate(0, UnaryOperator.identity())
                .limit(elements)
                .iterator(),
            executorService
        );
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return new TckCompatibleAsyncIteratorPublisher<>(
            () -> {
                throw new RuntimeException();
            },
            executorService
        );
    }

    @Override
    public long maxElementsFromPublisher() {
        return Integer.MAX_VALUE;
    }
}
