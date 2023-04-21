package demo.reactivestreams.part1c;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

@Test
public class IterablePublisherTest extends FlowPublisherVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public IterablePublisherTest() {
    super(new TestEnvironment());
  }

  @SuppressWarnings("unchecked")
  @Override public Flow.Publisher<Integer> createFlowPublisher(final long elements) {
    assert(elements <= maxElementsFromPublisher());
    return new NumberIterablePublisher(0, (int)elements, e);
  }

  @Override public Flow.Publisher<Integer> createFailedFlowPublisher() {
    return new AsyncIterablePublisher<Integer>(new Iterable<Integer>() {
      @Override public Iterator<Integer> iterator() {
        throw new RuntimeException("Error state signal!");
      }
    }, e);
  }

  @Override public long maxElementsFromPublisher() {
    return Integer.MAX_VALUE;
  }
}
