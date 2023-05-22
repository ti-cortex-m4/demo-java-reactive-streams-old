package demo.reactivestreams;

import demo.reactivestreams.part1.SyncPublisherSyncSubscriberRunner;
import demo.reactivestreams.part2.AsyncPublisherAsyncSubscriberRunner;
import demo.reactivestreams.part5.FileRunner;
import demo.reactivestreams.part5.WatchServiceRunner;

import java.io.IOException;
import java.util.concurrent.Executors;

public class Runner {

    public static void main(String[] args) throws InterruptedException, IOException {
//        SyncPublisherSyncSubscriberRunner.main(new String[]{});
//        AsyncPublisherAsyncSubscriberRunner.main(new String[]{});


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    FileRunner.main(new String[]{});
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        thread.start();

        WatchServiceRunner.main(new String[]{});

        thread.join();
    }
}
