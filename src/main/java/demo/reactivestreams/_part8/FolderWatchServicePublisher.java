package demo.reactivestreams._part8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;

public class FolderWatchServicePublisher extends SubmissionPublisher<FolderWatchEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatchServicePublisher.class);

    private final Future<?> task;

    FolderWatchServicePublisher(String folderName) {
        ExecutorService executorService = (ExecutorService) getExecutor();

        task = executorService.submit(() -> {
            try {
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

                        FolderWatchEvent item = new FolderWatchEvent(pathEvent, path);
                        logger.info("publisher.submit {}", item);
                        submit(item);
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }

                watchService.close();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void close() {
        task.cancel(false);
        super.close();
    }
}