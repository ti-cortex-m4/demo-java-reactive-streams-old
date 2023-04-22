package demo.reactivestreams._part3;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        TckCompatibleAsyncIterablePublisher<Integer> publisher = new TckCompatibleAsyncIterablePublisher<>(() -> list.iterator(),executorService);

        SyncSubscriber<Integer> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

//        SyncSubscriber<Integer> subscriber2 = new SyncSubscriber<>(2);
//        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
//        subscriber2.awaitCompletion();

        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
    }
}
