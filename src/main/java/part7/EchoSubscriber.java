package part7;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

class EchoSubscriber implements Flow.Subscriber<List<ByteBuffer>> {

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers) {
            System.out.println("onNext: " + StandardCharsets.UTF_8.decode(buffer));
        }
//                subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("onError: " + throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("onComplete");
    }
}
