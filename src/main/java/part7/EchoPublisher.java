package part7;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

public class EchoPublisher implements Flow.Publisher<ByteBuffer>{

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
        subscriber.onNext(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        subscriber.onComplete();
    }
}
