package demo.reactivestreams.part5;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class StringPublisher2 extends SubmissionPublisher<ByteBuffer> {

    private final String string;
    private Flow.Subscription subscription;

    public StringPublisher2(String string) {
        this.string = string;
    }

    public void onSubscribe(Flow.Subscription subscription) {
        (this.subscription = subscription).request(1);
    }

    public void onNext(ByteBuffer item) {
        subscription.request(1);
    }

    public void onError(Throwable ex) {
        closeExceptionally(ex);
    }

    public void onComplete() {
        close();
    }
}
