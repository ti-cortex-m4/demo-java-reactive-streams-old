package demo.reactivestreams._part4;

import demo.reactivestreams._part1.IteratorPublisher;

import java.util.List;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.copyOf(list).iterator());

        TckCompatibleSyncSubscriber<Integer> subscriber1 = new TckCompatibleSyncSubscriber<Integer>(1);
        publisher.subscribe(subscriber1);

        TckCompatibleSyncSubscriber<Integer> subscriber2 = new TckCompatibleSyncSubscriber<Integer>(2);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}
