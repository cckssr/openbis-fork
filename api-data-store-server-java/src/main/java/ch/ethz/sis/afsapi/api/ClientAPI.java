package ch.ethz.sis.afsapi.api;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public interface ClientAPI
{
    /*
        These methods solely exist as convenient methods over the Chunking methods
    */
    byte[] read(@NonNull String owner, @NonNull String source, @NonNull Long offset,
                @NonNull Integer limit) throws Exception;

    @NonNull
    Boolean write(@NonNull String owner, @NonNull String source, @NonNull Long offset,
                  @NonNull byte[] data) throws Exception;

    void resumeRead(@NonNull String owner, @NonNull String source, @NonNull Path destination, @NonNull Long offset) throws Exception;

    @NonNull
    Boolean resumeWrite(@NonNull String owner, @NonNull String destination, @NonNull Path source, @NonNull Long offset) throws Exception;

    @NonNull
    Boolean upload(@NonNull Path sourcePath, @NonNull String destinationOwner, @NonNull Path destinationPath, @NonNull FileCollisionListener fileCollisionListener, @NonNull TransferMonitorListener transferMonitorListener) throws Exception;

    @NonNull
    Boolean download(@NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath, @NonNull FileCollisionListener fileCollisionListener, @NonNull TransferMonitorListener transferMonitorListener) throws Exception;

    enum CollisionAction { Override, Resume, Skip };

    interface FileCollisionListener {
        CollisionAction collision(@NonNull Path sourcePath, @NonNull Path destinationPath);
    }

    interface TransferMonitorListener {
        void init(long start, long total);
        void add(Path from, long amount);

        // These methods are for implementations using event mechanisms
        void success();
        void failed(Exception ex);

        // These methods are for implementations using polling mechanisms
        boolean isFinished();
        Exception getException();
        long getCurrent();
        long getTotal();
    }

    FileCollisionListener overrideCollisionListener = new FileCollisionListener() {
        @Override
        public CollisionAction collision(@NonNull Path sourcePath, @NonNull Path destinationPath) {
            return CollisionAction.Override;
        }
    };

    class DefaultTransferMonitorLister implements TransferMonitorListener {
        private volatile Exception exception = null;
        private final AtomicLong current = new AtomicLong(0);
        private final AtomicLong total = new AtomicLong(0);

        @Override
        public void init(long start, long total) {
            this.total.set(total);
        }

        @Override
        public void add(Path from, long amount) {
            this.current.addAndGet(amount);
        }

        @Override
        public void success() {

        }

        @Override
        public void failed(Exception ex) {
            this.exception = ex;
        }

        @Override
        public boolean isFinished() {
            return current.get() == total.get() || exception != null;
        }

        @Override
        public Exception getException() {
            return exception;
        }

        @Override
        public long getCurrent() {
            return this.current.get();
        }

        @Override
        public long getTotal() {
            return this.total.get();
        }
    };

}
