package part6;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisherDemo_consume {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            CompletableFuture<Void> future = publisher.consume(System.out::println);
            LongStream.range(0, 10).forEach(publisher::submit);
            future.get();
        }
    }
}
