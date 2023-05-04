package demo.reactivestreams.part6;

import demo.reactivestreams.part4.RunnerSubmissionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

public class StringPublisher implements Flow.Publisher<ByteBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(StringPublisher.class);

    private final String string;

    public StringPublisher(String string) {
        this.string = string;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        logger.info("publisher.subscribe: {}", subscriber);

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
