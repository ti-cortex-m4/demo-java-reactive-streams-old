package part6;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisherDemo_subscribe {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            publisher.subscribe(new Flow.Subscriber<>() {

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                    System.out.println("subscribed: " + subscription);
                }

                @Override
                public void onNext(Long item) {
                    System.out.println("next: " + item);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("error: " + throwable);
                }

                @Override
                public void onComplete() {
                    System.out.println("completed");
                }
            });
            LongStream.range(0, 10).forEach(publisher::submit);
        }
    }
}
