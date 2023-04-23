package demo.reactivestreams._part5;

import demo.reactivestreams._part1.SyncIteratorPublisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;

@Test
public class TckCompatibleAsyncSubscriberBlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

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

    public TckCompatibleAsyncSubscriberBlackboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return new TckCompatibleAsyncSubscriber<>(0, executorService);
    }

    @Test
    public void testAccumulation() throws InterruptedException {
        AtomicLong accumulator = new AtomicLong();

        TckCompatibleAsyncSubscriber<Integer> subscriber = new TckCompatibleAsyncSubscriber<>(0, executorService) {
            @Override
            protected boolean whenNext(Integer element) {
                accumulator.addAndGet(element);
                return true;
            }
        };

        SyncIteratorPublisher<Integer> publisher = new SyncIteratorPublisher<>(() -> List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).iterator());
        publisher.subscribe(subscriber);

        subscriber.awaitCompletion();
        assertEquals(accumulator.get(), 45);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
