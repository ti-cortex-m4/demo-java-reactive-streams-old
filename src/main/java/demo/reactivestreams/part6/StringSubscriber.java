package demo.reactivestreams.part6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

class StringSubscriber implements Flow.Subscriber<List<ByteBuffer>> {

    private static final Logger logger = LoggerFactory.getLogger(StringSubscriber.class);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("subscriber.subscribe: {}", subscription);
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers) {
            logger.info("subscriber.subscribe: {}", StandardCharsets.UTF_8.decode(buffer));
        }
    }

    @Override
    public void onError(Throwable t) {
        logger.error("subscriber.error", t);
    }

    @Override
    public void onComplete() {
        logger.info("subscriber.complete");
    }
}
