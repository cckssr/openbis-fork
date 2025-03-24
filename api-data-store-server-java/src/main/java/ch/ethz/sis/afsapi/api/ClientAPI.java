package ch.ethz.sis.afsapi.api;

import lombok.NonNull;

import java.nio.file.Path;

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
}
