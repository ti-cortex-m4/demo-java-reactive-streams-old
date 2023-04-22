/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams._part1;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberWhiteboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test // Must be here for TestNG to find and run this, do not remove
public class SimpleSubscriberWhiteboxTest extends FlowSubscriberWhiteboxVerification<Integer> {

//  private ExecutorService e;
//  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
//  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SimpleSubscriberWhiteboxTest() {
    super(new TestEnvironment());
  }

  @Override
  public Flow.Subscriber<Integer> createFlowSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
    return new SyncSubscriber<Integer>(0) {
      @Override
      public void onSubscribe( Flow.Subscription s) {
        super.onSubscribe(s);

        probe.registerOnSubscribe(new SubscriberPuppet() {
          @Override
          public void triggerRequest(long elements) {
            s.request(elements);
          }

          @Override
          public void signalCancel() {
            s.cancel();
          }
        });
      }

      @Override
      public void onNext(Integer element) {
        super.onNext(element);
        probe.registerOnNext(element);
      }

      @Override
      public void onError(Throwable cause) {
        super.onError(cause);
        probe.registerOnError(cause);
      }

      @Override
      public void onComplete() {
        super.onComplete();
        probe.registerOnComplete();
      }

//      @Override
//      protected boolean whenNext(Integer element) {
//        return true;
//      }
    };
  }

  @Override public Integer createElement(int element) {
    return element;
  }

}
