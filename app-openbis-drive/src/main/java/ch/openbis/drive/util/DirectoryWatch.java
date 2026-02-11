package ch.openbis.drive.util;

import lombok.NonNull;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class DirectoryWatch {
    private final String localParentDirectory;
    private WatchService watcher;
    private final AtomicBoolean running = new AtomicBoolean();
    private Callable<Void> callback;
    private AtomicLong lastEventTs = new AtomicLong(-1);

    public DirectoryWatch(@NonNull String localParentDirectory) {
        this.localParentDirectory = localParentDirectory;
    }

    public synchronized void start(@NonNull Callable<Void> callback) throws Exception {
        //Try to close the service, in case the object is being restarted
        this.close();
        this.callback = callback;

        WatchService newWatcher = null;
        try {
            newWatcher = FileSystems.getDefault().newWatchService();
            register(newWatcher, Path.of(localParentDirectory));
            running.set(true);
            new Thread( this::processEvents ).start();
        } catch (Exception e) {
            if (newWatcher != null) {
                newWatcher.close();
            }
            running.set(false);
        }
        this.watcher = newWatcher;
    }

    void register(@NonNull WatchService watcher, @NonNull Path localParentDirectory) throws IOException {
        // register local directory and subdirectories
        Files.walkFileTree(localParentDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            return FileVisitResult.CONTINUE;
            }
        });
    }

    public synchronized void close() throws Exception {
        running.set(false);
        if (watcher != null) {
            watcher.close();
        }
    }

    public long getLastEventTs() {
        return lastEventTs.get();
    }

    void processEvents() {
        for (;;) {
            if (!running.get()) {
                return;
            }

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException | ClosedWatchServiceException x) {
                return;
            }

            if (key != null) {
                lastEventTs.set(System.currentTimeMillis());
                try {
                    callback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    this.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }
}

