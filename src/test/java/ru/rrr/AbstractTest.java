package ru.rrr;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public abstract class AbstractTest {
    protected final Supplier<Long> timeSupplier = System::currentTimeMillis;

    protected static final int PAUSE = 50;

    protected void waitFor(Supplier<Boolean> condition, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        final long startTime = timeSupplier.get();
        final long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        System.out.println(">>> timeoutMillis = " + timeoutMillis);
        while (!condition.get()) {
            if (timeSupplier.get() - startTime > timeoutMillis) {
                throw new TimeoutException();
            }
            TimeUnit.MILLISECONDS.sleep(PAUSE);
        }
    }
}
