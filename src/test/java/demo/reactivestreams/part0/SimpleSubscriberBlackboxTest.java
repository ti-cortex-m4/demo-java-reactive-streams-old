/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams.part0;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

@Test
public class SimpleSubscriberBlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

    public SimpleSubscriberBlackboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return new SimpleSubscriber<Integer>(1, new CountDownLatch(1), 1, 1);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
