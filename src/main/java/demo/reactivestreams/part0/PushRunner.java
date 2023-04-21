package demo.reactivestreams.part0;

import demo.reactivestreams.part1.SimpleSubscriber;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PushRunner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.copyOf(list).iterator());

        PushSubscriber<Integer> subscriber1 = new PushSubscriber<>(1);
        publisher.subscribe(subscriber1);

        PushSubscriber<Integer> subscriber2 = new PushSubscriber<>(2);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}
