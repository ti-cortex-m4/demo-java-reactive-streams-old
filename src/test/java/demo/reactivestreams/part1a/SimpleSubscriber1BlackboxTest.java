/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams.part1a;

import demo.reactivestreams.part1a.PullSubscriber;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class SimpleSubscriber1BlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

    public SimpleSubscriber1BlackboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return new PullSubscriber<Integer>();
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
