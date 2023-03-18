package part1;

import java.util.concurrent.Flow;

public class SimpleSubscriber implements Flow.Subscriber<Integer> {

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.println("onSubscribe " + subscription);
    }

    @Override
    public void onNext(Integer item) {
        System.out.println("item = [" + item + "]");
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("throwable = [" + throwable + "]");
    }

    @Override
    public void onComplete() {
        System.out.println("complete");
    }
}
