package part6;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class Chaining {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Integer> publisher1 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher2 = new SubmissionPublisher<>();
             SubmissionPublisher<Integer> publisher3 = new SubmissionPublisher<>()) {

            publisher1.consume(x -> publisher2.submit(x * x));
            publisher2.consume(x -> publisher3.submit(x * x));
            publisher3.consume(x -> System.out.println(x));

            publisher1.submit(1);
            publisher1.submit(2);
            publisher1.submit(3);

            ForkJoinPool.commonPool().awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
