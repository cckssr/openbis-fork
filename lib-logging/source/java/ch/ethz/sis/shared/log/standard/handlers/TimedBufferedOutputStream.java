package ch.ethz.sis.shared.log.standard.handlers;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimedBufferedOutputStream extends BufferedOutputStream {
    // Time of the last write in nanoseconds
    private final AtomicLong lastWrite = new AtomicLong(System.nanoTime());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * @param out        underlying output stream
     * @param size       buffer size in bytes
     * @param idleMillis flush if no write occurs for this many milliseconds
     */
    public TimedBufferedOutputStream(OutputStream out, int size, long idleMillis) {
        super(out, size);

        // Schedule a periodic check
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(now - lastWrite.get());
            if (elapsedMs >= idleMillis) {
                try {
                    flush();               // flush the buffered data
                } catch (IOException e) {
                    // handle or re‑throw as needed
                    e.printStackTrace();
                }
            }
        }, idleMillis, idleMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        super.write(b);
        lastWrite.set(System.nanoTime());
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        lastWrite.set(System.nanoTime());
    }

    @Override
    public void close() throws IOException {
        scheduler.shutdownNow();   // stop the background task
        super.close();
    }
}

