package demo.reactivestreams.part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTest {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    protected static void delay() {
        delay(1);
    }

    protected static void delay(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
