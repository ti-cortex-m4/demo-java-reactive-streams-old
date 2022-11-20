package part00;

import java.util.Observable;

public class ExampleReactiveExtensions {

    private final Observable observable = new Observable();

    public void example() {
        observable.addObserver((observable, arg) -> {
           Value value = (Value) arg;
        });
    }

    private static class Value {
    }
}
