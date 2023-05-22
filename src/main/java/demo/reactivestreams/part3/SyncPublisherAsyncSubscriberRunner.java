package demo.reactivestreams.part3;

import demo.reactivestreams.part1.SyncIteratorPublisher;
import demo.reactivestreams.part2.AsyncSubscriber;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SyncPublisherAsyncSubscriberRunner {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<String> words = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
        Supplier<Iterator<? extends String>> iteratorSupplier = () -> List.copyOf(words).iterator();
        SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(iteratorSupplier);

        AsyncSubscriber<String> subscriber1 = new AsyncSubscriber<>(1, executorService);
        publisher.subscribe(subscriber1);

        AsyncSubscriber<String> subscriber2 = new AsyncSubscriber<>(2, executorService);
        publisher.subscribe(subscriber2);

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();

        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
    }
}
