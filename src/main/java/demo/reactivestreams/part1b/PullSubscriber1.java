package demo.reactivestreams.part1b;

import demo.reactivestreams.part1a.AbstractSyncSubscriber;

public class PullSubscriber1<T> extends AbstractSyncSubscriber<T> {

    private int i = 0;

    @Override
    protected boolean whenNext(T element) {
        return ++i<5;
    }
}
