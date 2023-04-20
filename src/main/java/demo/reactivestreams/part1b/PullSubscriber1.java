package demo.reactivestreams.part1b;

import java.util.concurrent.Executor;

public class PullSubscriber1<T> extends AsyncSubscriber<T> {

    private int i = 0;

    protected PullSubscriber1(Executor executor) {
        super(executor);
    }

    @Override
    protected boolean whenNext(T element) {
        return ++i<5;
    }
}
