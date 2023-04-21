package demo.reactivestreams.part1a;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class TckCompatibleSyncSubscriberBlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

    public TckCompatibleSyncSubscriberBlackboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return new TckCompatibleSyncSubscriber<Integer>(0);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
