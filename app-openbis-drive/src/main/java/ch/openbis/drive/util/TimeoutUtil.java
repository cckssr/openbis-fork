package ch.openbis.drive.util;

import lombok.Getter;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutUtil {
    public static <T> T doWithTimeout(Callable<T> callable, int millisecondTimeout) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Result<T> result = new Result<>();

        new Thread( () -> {
            try{
                result.ok = callable.call();
            } catch (Exception e) {
                result.exception = e;
            } finally {
                countDownLatch.countDown();
            }
        }).start();

        if( countDownLatch.await(millisecondTimeout, TimeUnit.MILLISECONDS) ) {
            if (result.getException() == null) {
                return result.getOk();
            } else {
                throw result.getException();
            }
        } else {
            throw new TimeoutException(String.format("Exceeded %s milliseconds", millisecondTimeout));
        }
    }

    public static class Result<T> {
        @Getter
        volatile T ok;
        @Getter
        volatile Exception exception;
    }
}
