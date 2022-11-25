package _models;

import java.util.Optional;

public class ExampleIterator {

    private final Producer producer = new Producer();

    public void example() {
        Optional<Value> value = producer.getValue();
    }

    private static class Value {
    }

    private static class Producer {

        Optional<Value> getValue() {
            return null;
        }
    }
}
