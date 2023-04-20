package demo.reactivestreams.part1a;

public class TckCompatiblePullSubscriber<T> extends SyncSubscriber<T> {

    private int i = 0;

    @Override
    protected boolean whenNext(T element) {
        return ++i<5;
    }
}
