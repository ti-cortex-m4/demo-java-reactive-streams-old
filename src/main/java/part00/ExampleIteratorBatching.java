package part00;

import java.util.List;

public class ExampleIteratorBatching {

    private final Producer producer = new Producer();

    public void example() {
        List<Value> value = producer.getValues();
    }

    private static class Value {
    }

    private static class Producer {

        List<Value> getValues() {
            return null;
        }
    }
}
