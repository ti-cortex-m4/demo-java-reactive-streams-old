package demo.reactivestreams.part1a;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class TckCompatibleBlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

    public TckCompatibleBlackboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return new TckCompatiblePullSubscriber<Integer>();
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
