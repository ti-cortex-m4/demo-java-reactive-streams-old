package part00;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

public class ExampleReactiveExtensions {

    public static void main(String[] args) {
        Observer<String> observer = new Observer<>() {
            @Override
            public void onCompleted() {
                System.out.println("completed");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("error: " + throwable);
            }

            @Override
            public void onNext(String message) {
                System.out.println("received: " + message);
            }
        };
        Observable<String> observable = Observable.just("a", "b", "c");
        Subscription subscription = observable
            .filter(s -> !s.isEmpty())
            .map(s -> s.toUpperCase())
            .subscribe(observer);

        subscription.unsubscribe();
    }
}
