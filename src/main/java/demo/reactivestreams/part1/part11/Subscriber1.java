package demo.reactivestreams.part1.part11;

public class Subscriber1 extends SyncSubscriber<Integer> {

    private int i = 0;

    @Override
    protected boolean whenNext(Integer element) {
        return ++i<5;
    }
}
