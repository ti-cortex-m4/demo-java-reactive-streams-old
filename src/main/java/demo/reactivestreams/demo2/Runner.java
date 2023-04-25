package demo.reactivestreams.demo2;

import demo.reactivestreams.demo1.SyncIteratorPublisher;
import demo.reactivestreams.demo1.SyncSubscriber;
import demo.reactivestreams.demo4.AsyncSubscriber;

import java.util.List;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<String> list = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
        SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(list).iterator());

        AsyncSubscriber<String> subscriber1 = new AsyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

        AsyncSubscriber<String> subscriber2 = new AsyncSubscriber<>(2);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}
