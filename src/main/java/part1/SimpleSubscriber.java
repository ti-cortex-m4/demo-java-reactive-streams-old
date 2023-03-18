package part1;

import java.util.concurrent.Flow;

public class SimpleSubscriber implements Flow.Subscriber<Integer> {

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.println("onSubscribe: " + subscription);
    }

    @Override
    public void onNext(Integer item) {
        System.out.println("onNext: " + item);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("onError " + throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("onComplete");
    }
}
