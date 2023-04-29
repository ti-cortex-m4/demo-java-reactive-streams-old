package demo.reactivestreams.demo.demo3;

import demo.reactivestreams.demo.demo1.SyncIteratorPublisher;
import demo.reactivestreams.demo.demo2.AsyncSubscriber;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RunnerSyncPublisherAsyncSubscriber {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<String> list = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
        SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(list).iterator());

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