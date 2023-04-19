package demo.reactivestreams.part1;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PushRunner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        TckCompatibleIteratorPublisher<Integer> publisher = new TckCompatibleIteratorPublisher<>(() -> List.copyOf(list).iterator());

        CountDownLatch completeLatch1 = new CountDownLatch(1);
        Flow.Subscriber<Integer> subscriber1 = new SimpleSubscriber<>(1, completeLatch1,Long.MAX_VALUE,0);
        publisher.subscribe(subscriber1);

        CountDownLatch completeLatch2 = new CountDownLatch(1);
        Flow.Subscriber<Integer> subscriber2 = new SimpleSubscriber<>(2, completeLatch2,Long.MAX_VALUE,0);
        publisher.subscribe(subscriber2);

        completeLatch1.await();
        completeLatch2.await();
    }
}
