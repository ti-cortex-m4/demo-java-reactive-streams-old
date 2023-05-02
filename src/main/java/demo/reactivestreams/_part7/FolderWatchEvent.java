package demo.reactivestreams._part7;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.StringJoiner;

public class FolderWatchEvent {

    private final WatchEvent<Path> event;
    private final Path path;

    public FolderWatchEvent(WatchEvent<Path> event, Path path) {
        this.event = event;
        this.path = path;
    }

    public WatchEvent<Path> getEvent() {
        return event;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FolderWatchEvent.class.getSimpleName() + "[", "]")
            .add("event='" + event + "'")
            .add("path='" + path + "'")
            .toString();
    }
}
