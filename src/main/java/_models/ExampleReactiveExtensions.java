package _models;
/*
import rx.Observable;
import rx.Observer;
import rx.Subscription;
*/
public class ExampleReactiveExtensions {
/*
    public static void main(String[] args) {
        Observer<String> consumer = new Observer<>() {
            @Override
            public void onNext(String message) {
                System.out.println("data: " + message);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("error: " + throwable);
            }

            @Override
            public void onCompleted() {
                System.out.println("completed");
            }
        };
        Observable<String> producer = Observable.just("a", "b", "c");
        Subscription subscription = producer
            .filter(s -> !s.isEmpty())
            .map(s -> s.toUpperCase())
            .subscribe(consumer);

        subscription.unsubscribe();
    }

 */
}
