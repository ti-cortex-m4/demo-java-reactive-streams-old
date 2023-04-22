package demo.reactivestreams._part5;

import demo.reactivestreams._part1.SyncIteratorPublisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
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
        return new TckCompatibleAsyncSubscriber<Integer>(0, executorService) {
            @Override
            protected boolean whenNext(final Integer element) {
                return true;
            }
        };
    }

    @Test
    public void testAccumulation() throws InterruptedException {

        final AtomicLong i = new AtomicLong(Long.MIN_VALUE);
        final CountDownLatch latch = new CountDownLatch(1);
        final Flow.Subscriber<Integer> sub = new TckCompatibleAsyncSubscriber<Integer>(0, executorService) {
            private long acc;

            @Override
            protected boolean whenNext(final Integer element) {
                acc += element;
                return true;
            }

            @Override
            protected void whenComplete() {
                i.set(acc);
                latch.countDown();
            }
        };

        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        SyncIteratorPublisher<Integer> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(list).iterator());
        publisher.subscribe(sub);
//    new NumberIterablePublisher(0, 10, e).subscribe(sub);
        latch.await(env.defaultTimeoutMillis() * 10, TimeUnit.MILLISECONDS);
        assertEquals(i.get(), 45);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

}
