package demo.reactivestreams.part5;

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

public class PathWatchServiceSubmissionPublisher extends SubmissionPublisher<WatchEvent<Path>> {

    private static final Logger logger = LoggerFactory.getLogger(PathWatchServiceSubmissionPublisher.class);

    private final Future<?> task;

    PathWatchServiceSubmissionPublisher(String folderName) {
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

                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;

                        logger.info("publisher.submit {}", watchEvent);
                        submit(watchEvent);
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
        logger.info("publisher.close");
        task.cancel(false);
        super.close();
    }
}
