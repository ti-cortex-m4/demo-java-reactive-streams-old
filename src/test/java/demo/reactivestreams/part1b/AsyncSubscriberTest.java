package demo.reactivestreams.part1b;

import demo.reactivestreams.part0.IteratorPublisher;
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

@Test // Must be here for TestNG to find and run this, do not remove
public class AsyncSubscriberTest extends FlowSubscriberBlackboxVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public AsyncSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override public Flow.Subscriber<Integer> createFlowSubscriber() {
    return new AsyncSubscriber<Integer>(e) {
      @Override protected boolean whenNext(final Integer element) {
        return true;
      }
    };
  }

  @Test public void testAccumulation() throws InterruptedException {

    final AtomicLong i = new AtomicLong(Long.MIN_VALUE);
    final CountDownLatch latch = new CountDownLatch(1);
    final Flow.Subscriber<Integer> sub =  new AsyncSubscriber<Integer>(e) {
      private long acc;
      @Override protected boolean whenNext(final Integer element) {
        acc += element;
        return true;
      }

      @Override protected void whenComplete() {
        i.set(acc);
        latch.countDown();
      }
    };

    List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.copyOf(list).iterator());
    publisher.subscribe(sub);
//    new NumberIterablePublisher(0, 10, e).subscribe(sub);
    latch.await(env.defaultTimeoutMillis() * 10, TimeUnit.MILLISECONDS);
    assertEquals(i.get(), 45);
  }

  @Override public Integer createElement(int element) {
    return element;
  }

}
