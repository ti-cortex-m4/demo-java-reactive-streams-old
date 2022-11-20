package part00;

import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

public class ExampleObserver {

    private final Observable observable = new Observable();

    public void example() {
        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                Value value = (Value) arg;
            }
        };
        observable.addObserver(observer);
        Value value = new Value();
        observable.notifyObservers(value);
        //
        observable.deleteObserver(observer);
    }

    private static class Value {
    }
}
