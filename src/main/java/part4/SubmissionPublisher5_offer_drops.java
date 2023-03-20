package part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.LongStream;

public class SubmissionPublisher5_offer_drops {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPublisher5_offer_drops.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 2)) {
            System.out.println("getMaxBufferCapacity: " + publisher.getMaxBufferCapacity());

            CompletableFuture<Void> future = publisher.consume(item -> {
//                logger.info("before consume: " + item);
                delay();
                logger.info("consumed: " + item);
            });

            LongStream.range(0, 10).forEach(item -> {
//                    logger.info("before offer: " + item);
                    publisher.offer(item, new BiPredicate<Flow.Subscriber<? super Long>, Long>() {
                        @Override
                        public boolean test(Flow.Subscriber<? super Long> subscriber, Long aLong) {
                            logger.info("dropped: " + aLong);
                            delay();
                            return false;
                        }
                    });
//                    logger.info("after offer:  " + item);
                }
            );
            future.get();
        }
    }

    private static void delay() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
