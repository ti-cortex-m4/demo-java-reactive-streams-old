package part5;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

public class StringPublisher implements Flow.Publisher<ByteBuffer>{

    private final String string;

    public StringPublisher(String string) {
        this.string = string;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });
        subscriber.onNext(ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8)));
        subscriber.onComplete();
    }
}
