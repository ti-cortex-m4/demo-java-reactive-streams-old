package part6;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.LongStream;

public class SubmissionPublisherDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<Long>()) {
            System.out.println("Subscriber Buffer Size: " + publisher.getMaxBufferCapacity());
            CompletableFuture<Void> future = publisher.consume(System.out::println);
            LongStream.range(10, 20).forEach(publisher::submit);
            future.get();
        }
    }
}
