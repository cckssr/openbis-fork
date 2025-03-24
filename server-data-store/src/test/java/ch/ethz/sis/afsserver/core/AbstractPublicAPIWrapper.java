/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afsserver.core;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.ethz.sis.afsapi.api.PublicAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsclient.client.ChunkEncoderDecoder;
import lombok.NonNull;

public abstract class AbstractPublicAPIWrapper implements PublicAPI
{

    public abstract <E> E process(Class<E> responseType, String method, Map<String, Object> params, byte[] data);

    public <E> E process(Class<E> responseType, String method, Map<String, Object> params)
    {
        return  process(responseType,method,params, null);
    }

    @Override
    public List<File> list(@NonNull String owner, @NonNull String source,
            @NonNull Boolean recursively) throws Exception
    {
        Map<String, Object> args = Map.of(
                "owner", owner,
                "source", source,
                "recursively", recursively);
        return process(List.class, "list", args);
    }

    @Override
    public Chunk[] read(@NonNull Chunk[] chunks) throws Exception
    {
        Map<String, Object> args = Map.of();
        return process(Chunk[].class,"read", args, ChunkEncoderDecoder.encodeChunksAsBytes(chunks));
    }

    @Override
    public Boolean write(@NonNull Chunk[] chunks) throws Exception
    {
        Map<String, Object> args = Map.of();
        return process(Boolean.class, "write", args, ChunkEncoderDecoder.encodeChunksAsBytes(chunks));
    }

    @Override
    public Boolean delete(@NonNull String owner, @NonNull String source) throws Exception
    {
        Map<String, Object> args = Map.of(
                "owner", owner,
                "source", source);
        return process(Boolean.class, "delete", args);
    }

    @Override
    public Boolean copy(@NonNull String sourceOwner, @NonNull String source,
            @NonNull String targetOwner, @NonNull String target) throws Exception
    {
        Map<String, Object> args = Map.of(
                "sourceOwner", sourceOwner,
                "source", source,
                "targetOwner", targetOwner,
                "target", target);
        return process(Boolean.class,"copy", args);
    }

    @Override
    public Boolean move(@NonNull String sourceOwner, @NonNull String source,
            @NonNull String targetOwner, @NonNull String target) throws Exception
    {
        Map<String, Object> args = Map.of(
                "sourceOwner", sourceOwner,
                "source", source,
                "targetOwner", targetOwner,
                "target", target);
        return process(Boolean.class, "move", args);
    }

    @Override
    public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception
    {
        final Map<String, Object> args = Map.of(
                "owner", owner,
                "source", source,
                "directory", directory);
        return process(Boolean.class, "create", args);
    }

    @Override
    public @NonNull FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        final Map<String, Object> args = Map.of(
                "owner", owner,
                "source", source);
        return process(FreeSpace.class, "free", args);
    }

    @Override
    public void begin(UUID transactionId) throws Exception
    {
        Map<String, Object> args = Map.of(
                "transactionId", transactionId);
        process(null, "begin", args);
    }

    @Override
    public Boolean prepare() throws Exception
    {
        Map<String, Object> args = Map.of();
        return process(Boolean.class, "prepare", args);
    }

    @Override
    public void commit() throws Exception
    {
        Map<String, Object> args = Map.of();
        process(null, "commit", args);
    }

    @Override
    public void rollback() throws Exception
    {
        Map<String, Object> args = Map.of();
        process(null, "rollback", args);
    }

    @Override
    public List<UUID> recover() throws Exception
    {
        Map<String, Object> args = Map.of();
        return process(List.class, "recover", args);
    }

    @Override
    public String login(String userId, String password) throws Exception
    {
        Map<String, Object> args = Map.of(
                "userId", userId,
                "password", password);
        return process(String.class, "login", args);
    }

    @Override
    public Boolean isSessionValid() throws Exception
    {
        Map<String, Object> args = Map.of();
        return process(Boolean.class, "isSessionValid", args);
    }

    @Override
    public Boolean logout() throws Exception
    {
        Map<String, Object> args = Map.of();
        return process(Boolean.class, "logout", args);
    }
}
