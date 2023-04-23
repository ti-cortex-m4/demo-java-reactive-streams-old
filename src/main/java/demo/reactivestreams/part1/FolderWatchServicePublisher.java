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
import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;

public class FolderWatchServicePublisher extends SubmissionPublisher<Message> {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatchServicePublisher.class);

    private final Future<?> task;

    FolderWatchServicePublisher(String folderName) {
        ExecutorService executorService = (ExecutorService)getExecutor();

        task =  executorService.submit(() -> {
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

                        Message message = new Message(pathEvent, path);
                        logger.info("publisher.submit {}", message);
                        submit(message);
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }

                watchService.close();
            } catch (Exception e) {
                logger.error("Folder watch service failed", e);
            }
        });
    }

    @Override
    public void close() {
        task.cancel(false);
        super.close();
    }
}
