package demo.reactivestreams._part8;

import demo.reactivestreams.demo.demo1.SyncSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SubmissionPublisher;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<FolderWatchEvent> publisher = new FolderWatchServicePublisher(System.getProperty("user.home"))) {

            SyncSubscriber<FolderWatchEvent> subscriber1 = new SyncSubscriber<>(1);
            publisher.subscribe(subscriber1);

//        SyncSubscriber<Integer> subscriber2 = new SyncSubscriber<>(2);
//        publisher.subscribe(subscriber2);
//
//        Iterator<Integer> iterator = IntStream.rangeClosed(0, 9).iterator();
//        iterator.forEachRemaining(item -> {
//            logger.info("publisher.submit: {}", item);
//            publisher.submit(item);
//        });
//
//        logger.info("publisher.close");
//        publisher.close();

            subscriber1.awaitCompletion();
        }
//        subscriber2.awaitCompletion();
    }
}
