package demo.reactivestreams.part1b;

import demo.reactivestreams._part1.SyncIteratorPublisher;
import org.reactivestreams.Publisher;

import java.util.List;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<Integer> list = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        Publisher<Integer> publisher = new RangePublisher(0,3);

        SyncSubscriber<Integer> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

//        SyncSubscriber<Integer> subscriber2 = new SyncSubscriber<>(2);
//        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
//        subscriber2.awaitCompletion();
    }
}
