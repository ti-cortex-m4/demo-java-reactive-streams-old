package demo.reactivestreams.part1;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.stream.IntStream;

public class PullRunner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.copyOf(list).iterator());

        CountDownLatch completeLatch1 = new CountDownLatch(1);
        Flow.Subscriber<Integer> subscriber1 = new PullSubscriber<>(1,completeLatch1);
        publisher.subscribe(subscriber1);

        CountDownLatch completeLatch2 = new CountDownLatch(1);
        Flow.Subscriber<Integer> subscriber2 = new PullSubscriber<>(2,completeLatch2);
        publisher.subscribe(subscriber2);

        completeLatch1.await();
        completeLatch2.await();
    }
}
