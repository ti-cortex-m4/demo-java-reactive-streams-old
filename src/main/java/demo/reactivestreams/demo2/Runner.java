package demo.reactivestreams.demo2;

import demo.reactivestreams._part1.SyncIteratorPublisher;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        AsyncIteratorPublisher<Integer> publisher = new AsyncIteratorPublisher<>(() -> List.copyOf(list).iterator(), executorService);

        AsyncSubscriber<Integer> subscriber1 = new AsyncSubscriber<Integer>(1, executorService);
        publisher.subscribe(subscriber1);

        AsyncSubscriber<Integer> subscriber2 = new AsyncSubscriber<Integer>(2, executorService);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();

        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
    }
}
