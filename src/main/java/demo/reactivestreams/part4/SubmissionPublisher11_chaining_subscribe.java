package demo.reactivestreams.part4;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class SubmissionPublisher11_chaining_subscribe extends AbstractTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>()) {

            Processor step1 = new Processor("step1");
            Processor step2 = new Processor("step2");
            Processor step3 = new Processor("step3");

            step2.subscribe(step3);
            step1.subscribe(step2);
            publisher.subscribe(step1);

            publisher.submit(1);
            publisher.submit(2);
            publisher.submit(3);
            publisher.close();

            logger.info("finished");

            ExecutorService executorService = (ExecutorService) publisher.getExecutor();
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    static class Processor extends SubmissionPublisher<Integer> implements Flow.Processor<Integer, Integer> {

        private final String name;

        private Flow.Subscription subscription;

        Processor(String name) {
            this.name = name;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            logger.info("{}.onSubscribe: {}", name, subscription);
            this.subscription = subscription;
            this.subscription.request(1);
        }

        @Override
        public void onNext(Integer item) {
            delay();
            logger.info("{}.onNext: {}", name, item);
            submit(item * 10);
            this.subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("{}.onError", name, throwable);
            closeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("{}.onComplete", name);
            close();
        }
    }
}
