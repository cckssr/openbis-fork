package ch.openbis.drive.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelExecutionUtil {
    public final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

}
