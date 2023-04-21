package demo.reactivestreams.part1c;

import demo.reactivestreams.part1.TckCompatibleIteratorPublisher;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        NumberIterablePublisher publisher = new NumberIterablePublisher(0,9,executorService);

        SyncSubscriber<Integer> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

//        SyncSubscriber<Integer> subscriber2 = new SyncSubscriber<>(2);
//        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
//        subscriber2.awaitCompletion();
    }
}
