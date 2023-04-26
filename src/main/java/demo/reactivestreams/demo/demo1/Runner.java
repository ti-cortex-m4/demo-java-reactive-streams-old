package demo.reactivestreams.demo.demo1;

import java.util.List;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        List<String> list = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
        SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(list).iterator());

        SyncSubscriber<String> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

        SyncSubscriber<String> subscriber2 = new SyncSubscriber<>(2);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}
