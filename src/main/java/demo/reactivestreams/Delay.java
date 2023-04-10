package demo.reactivestreams;

import java.util.concurrent.TimeUnit;

public final class Delay {

    public static void delay() {
        delay(1);
    }

    public static void delay(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
