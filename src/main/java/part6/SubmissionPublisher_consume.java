package part6;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisher_consume {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {
            System.out.println("getExecutor: " + publisher.getExecutor());
            System.out.println("getMaxBufferCapacity: " + publisher.getMaxBufferCapacity());

            CompletableFuture<Void> future = publisher.consume(System.out::println);
            System.out.println("getNumberOfSubscribers: " + publisher.getNumberOfSubscribers());

            LongStream.range(0, 10).forEach(publisher::submit);

            future.get();
        }
    }
}
