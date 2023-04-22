package demo.reactivestreams._part5;

import demo.reactivestreams._part1.IteratorPublisher;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PullRunner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.copyOf(list).iterator());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        TckCompatibleAsyncSubscriber<Integer> subscriber1 = new TckCompatibleAsyncSubscriber<Integer>(1, executorService);
        publisher.subscribe(subscriber1);

        TckCompatibleAsyncSubscriber<Integer> subscriber2 = new TckCompatibleAsyncSubscriber<Integer>(2, executorService);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();

        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
    }
}
