package demo.reactivestreams.part11;

import org.reactivestreams.example.unicast.subscriber.SyncSubscriber;

public class Subscriber1 extends SyncSubscriber<Integer> {

    private int i = 0;

    @Override
    protected boolean whenNext(Integer element) {
        return ++i<5;
    }
}
