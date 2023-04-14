package demo.reactivestreams.part1;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PushRunner {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch completeLatch = new CountDownLatch(1);

        Iterator<Integer> iterator = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).iterator();
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> iterator);

        Flow.Subscriber<Integer> subscriber = new PushSubscriber<>(completeLatch);
        publisher.subscribe(subscriber);

        completeLatch.await();
    }
}
