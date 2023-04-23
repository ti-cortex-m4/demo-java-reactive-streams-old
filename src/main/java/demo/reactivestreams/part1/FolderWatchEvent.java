package demo.reactivestreams.part1;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.StringJoiner;

public class FolderWatchEvent {

    private final String action;
    private final String path;

    public FolderWatchEvent(WatchEvent<Path> event, Path path) {
        this.action = event.kind().toString();
        this.path = path.toString();
    }

    public String getAction() {
        return action;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FolderWatchEvent.class.getSimpleName() + "[", "]")
            .add("action='" + action + "'")
            .add("path='" + path + "'")
            .toString();
    }
}