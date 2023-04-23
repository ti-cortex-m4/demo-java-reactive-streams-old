package demo.reactivestreams._part4;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberWhiteboxVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class TckCompatibleSyncSubscriberWhiteboxTest extends FlowSubscriberWhiteboxVerification<Integer> {

    public TckCompatibleSyncSubscriberWhiteboxTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new TckCompatibleSyncSubscriber<>(0) {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
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
        };
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
