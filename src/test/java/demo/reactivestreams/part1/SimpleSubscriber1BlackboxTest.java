/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams.part1;

import demo.reactivestreams.part1.part11.Subscriber1;
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
        return new Subscriber1();
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
