package ch.ethz.sis.afsapi.api;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @NonNull
    Boolean upload(@NonNull Path sourcePath, @NonNull String destinationOwner, @NonNull Path destinationPath, @NonNull FileCollisionListener fileCollisionListener, @NonNull TransferMonitorListener transferMonitorListener) throws Exception;

    @NonNull
    Boolean download(@NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath, @NonNull FileCollisionListener fileCollisionListener, @NonNull TransferMonitorListener transferMonitorListener) throws Exception;

    enum CollisionAction { Override, Resume, Skip };

    interface FileCollisionListener {
        CollisionAction precheck(@NonNull Path sourcePath, @NonNull Path destinationPath, boolean collision);
    }

    interface FileTransferredListener {
        void transferred(@NonNull Path sourcePath, @NonNull Path destinationPath);
    }

    interface TransferMonitorListener {
        void init(long start, long total);

        void start(Path from, Path to, long total);
        void stop();
        void add(Path from, Path to, long amount, boolean completed);

        // These methods are for implementations using event mechanisms
        void success();
        void failed(Exception ex);

        // Listeners for intelligent file upload/download management
        void addFileTransferredListener(FileTransferredListener fileTransferredListener);

        // These methods are for implementations using polling mechanisms
        boolean isFinished();
        boolean isStop();
        Exception getException();
        long getCurrent();
        long getTotal();
    }

    FileCollisionListener overrideCollisionListener = new FileCollisionListener() {
        @Override
        public CollisionAction precheck(@NonNull Path sourcePath, @NonNull Path destinationPath, boolean collision) {
            return CollisionAction.Override;
        }
    };

    class DefaultTransferMonitorLister implements TransferMonitorListener {
        private volatile Exception exception = null;
        private final AtomicLong current = new AtomicLong(0);
        private final AtomicLong total = new AtomicLong(0);
        private final List<FileTransferredListener> fileTransferredListeners = new ArrayList<>();
        private volatile boolean stop = false;

        @Override
        public void init(long start, long total) {
            this.total.set(total);
        }

        @Override
        public synchronized void start(Path from, Path to, long total) {}

        @Override
        public synchronized void stop() {
            stop = true;
        }

        @Override
        public boolean isStop() {
            return stop;
        }

        @Override
        public synchronized void add(Path from, Path to, long amount, boolean completed) {
            this.current.addAndGet(amount);
            if(completed) {
                for (FileTransferredListener fileTransferredListener:fileTransferredListeners) {
                    fileTransferredListener.transferred(from, to);
                }
            }
        }

        @Override
        public void success() {

        }

        @Override
        public void failed(Exception ex) {
            this.exception = ex;
        }

        @Override
        public void addFileTransferredListener(FileTransferredListener fileTransferredListener) {
            fileTransferredListeners.add(fileTransferredListener);
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
