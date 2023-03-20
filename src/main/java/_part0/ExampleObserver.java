package _part0;

import java.util.Observable;
import java.util.Observer;

public class ExampleObserver {


    public static void main(String[] args) {
        Observable producer = new Observable();

        Observer consumer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("received: " + arg);
            }
        };
        producer.addObserver(consumer);

        producer.notifyObservers("a");
        producer.notifyObservers("b");
        producer.notifyObservers("c");
        //
        producer.deleteObserver(consumer);
    }

    private static class Value {
    }
}
