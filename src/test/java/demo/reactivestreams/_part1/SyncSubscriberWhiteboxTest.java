package demo.reactivestreams._part1;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberWhiteboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class SyncSubscriberWhiteboxTest extends FlowSubscriberWhiteboxVerification<Integer> {

    public SyncSubscriberWhiteboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new SyncSubscriber<>(0) {
            @Override
            public void onSubscribe(Flow.Subscription s) {
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
        };
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}

