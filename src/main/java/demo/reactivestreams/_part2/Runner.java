package demo.reactivestreams._part2;

import demo.reactivestreams._part1.SyncSubscriber;

import java.util.List;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        TckCompatibleSyncIteratorPublisher<Integer> publisher = new TckCompatibleSyncIteratorPublisher<>(() -> List.copyOf(list).iterator());

        SyncSubscriber<Integer> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

        SyncSubscriber<Integer> subscriber2 = new SyncSubscriber<>(2);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}