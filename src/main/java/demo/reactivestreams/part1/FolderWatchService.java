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
import java.util.function.Consumer;

//System.getProperty("user.home")
public class FolderWatchService {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatchService.class);

    private final Consumer<Event> consumer;

    public FolderWatchService(Consumer<Event> consumer) {
        this.consumer = consumer;
    }

    public void start(String folderName) {
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(() -> {
            try {
                logger.info("Folder watch service started");

                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path folder = Paths.get(folderName);
                folder.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path path = folder.resolve(pathEvent.context());

                        logger.info("Folder change event is published: {}", pathEvent);
                        consumer.accept(new Event(pathEvent, path));
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
    }
}
