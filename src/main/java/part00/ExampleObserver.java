package part00;

import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

public class ExampleObserver {


    public static void main(String[] args) {
        Observable observable = new Observable();

        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("received: " + arg);
            }
        };
        observable.addObserver(observer);

        observable.notifyObservers("a");
        observable.notifyObservers("b");
        observable.notifyObservers("c");
        //
        observable.deleteObserver(observer);
    }

    private static class Value {
    }
}
