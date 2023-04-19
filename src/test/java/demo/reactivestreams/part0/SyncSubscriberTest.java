/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams.part0;

import org.reactivestreams.Subscriber;
import org.reactivestreams.example.unicast.subscriber.SyncSubscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

@Test // Must be here for TestNG to find and run this, do not remove
public class SyncSubscriberTest extends FlowSubscriberBlackboxVerification<Integer> {

//  private ExecutorService e;
//  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
//  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SyncSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override public Flow.Subscriber<Integer> createFlowSubscriber() {
    return new SimpleSubscriber<Integer>(1, new CountDownLatch(1), 1,1);
//    return new SyncSubscriber<Integer>() {
//      private long acc;
//      @Override protected boolean whenNext(final Integer element) {
//        acc += element;
//        return true;
//      }
//
//      @Override public void onComplete() {
//        System.out.println("Accumulated: " + acc);
//      }
//    };
  }

  @Override public Integer createElement(int element) {
    return element;
  }
}
