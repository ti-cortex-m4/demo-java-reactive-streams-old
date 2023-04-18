package demo.reactivestreams.part1;

import demo.reactivestreams.Delay;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PushRunner {

    public static void main(String[] args) throws InterruptedException {
        Iterator<Integer> iterator = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).iterator();
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> iterator);

        CountDownLatch completeLatch1 = new CountDownLatch(1);
        Flow.Subscriber<Integer> subscriber1 = new PushSubscriber<>(completeLatch1);
        publisher.subscribe(subscriber1);

        //Delay.delay(5);

        CountDownLatch completeLatch2 = new CountDownLatch(1);
        Flow.Subscriber<Integer> subscriber2 = new PushSubscriber<>(completeLatch2);
        publisher.subscribe(subscriber2);

        completeLatch1.await();
        completeLatch2.await();
    }
}
