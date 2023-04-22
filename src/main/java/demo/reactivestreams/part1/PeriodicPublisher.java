package demo.reactivestreams.part1;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PeriodicPublisher extends SubmissionPublisher<Event> {

//    private final ScheduledFuture<?> periodicTask;
//    private final ScheduledExecutorService scheduler;

    PeriodicPublisher(Executor executor, int maxBufferCapacity
//        ,
//                      Supplier<? extends T> supplier,
//                      long period, TimeUnit unit
    ) {
        super(executor, maxBufferCapacity);
        new FolderWatchService(event -> submit(event)).start(System.getProperty("user.home"));
//        scheduler = new ScheduledThreadPoolExecutor(1);
//        periodicTask = scheduler.scheduleAtFixedRate(() -> submit(supplier.get()), 0, period, unit);
    }

    @Override
    public void close() {
//        periodicTask.cancel(false);
//        scheduler.shutdown();
        super.close();
    }
}
