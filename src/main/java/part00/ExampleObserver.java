package part00;

import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

public class ExampleObserver {

    private final Observable observable = new Observable();

    public void example() {
        observable.addObserver((observable, arg) -> {
           Value value = (Value) arg;
        });
    }

    private static class Value {
    }
}
