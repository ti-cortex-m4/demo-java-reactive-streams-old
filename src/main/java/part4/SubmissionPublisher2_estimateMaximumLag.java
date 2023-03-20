
package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SubmissionPublisher2_estimateMaximumLag extends SomeTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            publisher.subscribe(new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                    System.out.println("subscribed: " + subscription);
                }

                @Override
                public void onNext(Long item) {
                    delay(item.intValue());
                    this.subscription.request(1);

                    System.out.println("next: " + item);
                    System.out.println("estimateMaximumLag: " + publisher.estimateMaximumLag());
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

            ( (ExecutorService)publisher.getExecutor()).awaitTermination(10, TimeUnit.SECONDS);
//            publisher.close();
        }

    }
}
