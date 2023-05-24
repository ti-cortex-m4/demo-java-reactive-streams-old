package demo.reactivestreams;

import demo.reactivestreams.part1.SyncPublisherSyncSubscriberRunner;
import demo.reactivestreams.part2.AsyncPublisherAsyncSubscriberRunner;
import demo.reactivestreams.part5.FileModificationRunner;
import demo.reactivestreams.part5.WatchServiceRunner;

import java.io.IOException;

public class Runner {

    public static void main(String[] args) throws InterruptedException, IOException {
        SyncPublisherSyncSubscriberRunner.main(new String[]{});
        AsyncPublisherAsyncSubscriberRunner.main(new String[]{});

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    FileModificationRunner.main(new String[]{});
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
