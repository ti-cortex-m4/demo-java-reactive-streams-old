package part4;

import java.util.concurrent.TimeUnit;

public class SomeTest {

    protected static void delay() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
