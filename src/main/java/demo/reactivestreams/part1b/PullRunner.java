package demo.reactivestreams.part1b;

import demo.reactivestreams.Delay;
import demo.reactivestreams.part0.IteratorPublisher;
import demo.reactivestreams.part1a.TckCompatiblePullSubscriber;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class PullRunner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.copyOf(list).iterator());

//        CountDownLatch completeLatch1 = new CountDownLatch(1);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AsyncSubscriber<Integer> subscriber1 = new AsyncSubscriber<Integer>(executorService); //new SimpleSubscriber<>(1, completeLatch1,1,1);
        publisher.subscribe(subscriber1);

//        CountDownLatch completeLatch2 = new CountDownLatch(1);
//        Flow.Subscriber<Integer> subscriber2 = new SimpleSubscriber<>(2, completeLatch2,1,1);
//        publisher.subscribe(subscriber2);

//        Delay.delay(5);
        subscriber1.getCompletedAwait();

        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
//        completeLatch1.await();
//        completeLatch2.await();
    }
}
