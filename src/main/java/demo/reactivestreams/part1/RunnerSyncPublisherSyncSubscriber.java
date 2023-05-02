package demo.reactivestreams.part1;

import java.util.List;

public class RunnerSyncPublisherSyncSubscriber {

    public static void main(String[] args) throws InterruptedException {
        List<String> words = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
        SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(words).iterator());

        SyncSubscriber<String> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

        SyncSubscriber<String> subscriber2 = new SyncSubscriber<>(2);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}
