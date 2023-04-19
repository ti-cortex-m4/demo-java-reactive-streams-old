package demo.reactivestreams.part1a;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberWhiteboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test // Must be here for TestNG to find and run this, do not remove
public class TckCompatibleWhiteboxTest extends FlowSubscriberWhiteboxVerification<Integer> {

//  private ExecutorService e;
//  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
//  @AfterClass void after() { if (e != null) e.shutdown(); }

  public TckCompatibleWhiteboxTest() {
    super(new TestEnvironment());
  }

  @Override
  public Flow.Subscriber<Integer> createFlowSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
    return new TckCompatiblePullSubscriber<Integer>() {
      @Override
      public void onSubscribe( Flow.Subscription subscription) {
        super.onSubscribe(subscription);

        probe.registerOnSubscribe(new SubscriberPuppet() {
          @Override
          public void triggerRequest(long elements) {
            subscription.request(elements);
          }

          @Override
          public void signalCancel() {
            subscription.cancel();
          }
        });
      }

      @Override
      public void onNext(Integer element) {
        super.onNext(element);
        probe.registerOnNext(element);
      }

      @Override
      public void onError(Throwable throwable) {
        super.onError(throwable);
        probe.registerOnError(throwable);
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
