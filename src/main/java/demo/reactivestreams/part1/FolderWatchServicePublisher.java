package demo.reactivestreams.part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;

public class FolderWatchServicePublisher extends SubmissionPublisher<FolderWatchEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatchServicePublisher.class);

    private final Future<?> task;
//    private final ScheduledFuture<?> periodicTask;
//    private final ScheduledExecutorService scheduler;

    FolderWatchServicePublisher(String folderName
//        Executor executor, int maxBufferCapacity
//        ,
//                      Supplier<? extends T> supplier,
//                      long period, TimeUnit unit
    ) {
        super();
       // super(executor, maxBufferCapacity);
        ExecutorService executorService = (ExecutorService)getExecutor();
        task =  executorService.submit(() -> {
            try {
                logger.info("Folder watch service started");

                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path folder = Paths.get(folderName);
                folder.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                );

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path path = folder.resolve(pathEvent.context());

                        //logger.info("Folder change event is published: {}", pathEvent);

                        logger.info("publisher.submit {}", pathEvent);
                        submit(new FolderWatchEvent(pathEvent, path));
                        //eventPublisher.publishEvent(new FolderChangeEvent(this, pathEvent, path));
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }

                watchService.close();
                logger.info("Folder watch service finished");
            } catch (Exception e) {
                logger.error("Folder watch service failed", e);
            }
        });

//        new FolderWatchService(event -> submit(event)).start(System.getProperty("user.home"));
//        scheduler = new ScheduledThreadPoolExecutor(1);
//        periodicTask = scheduler.scheduleAtFixedRate(() -> submit(supplier.get()), 0, period, unit);
    }

    @Override
    public void close() {
        task.cancel(false);
//        scheduler.shutdown();
        super.close();
    }
}
