/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.parallelizedExecutor;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ch.ethz.sis.afsapi.api.PublicAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;

final class InMemoryAfsApi implements PublicAPI
{
    private static final String TRASH_PATH = "/.afs.trash";

    private static final String SNAPSHOTS_DIRECTORY = ".afs.snapshots";

    private static final OffsetDateTime LAST_MODIFIED = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    private final Map<String, Store> stores = new HashMap<>();

    private int snapshotNumber;

    private boolean failReads;

    private int busyListFailures;

    private final Set<String> readSources = new HashSet<>();

    private final Set<String> fileMutationPaths = new HashSet<>();

    private final Set<String> copySources = new HashSet<>();

    void addFile(String owner, String path, byte[] content)
    {
        Store store = store(owner);
        String normalizedPath = normalize(path);
        addParentDirectories(store, normalizedPath);
        store.files.put(normalizedPath, Arrays.copyOf(content, content.length));
        fileMutationPaths.add(normalizedPath);
    }

    void setFailReads(boolean failReads)
    {
        this.failReads = failReads;
    }

    void setBusyListFailures(int busyListFailures)
    {
        this.busyListFailures = busyListFailures;
    }

    Set<String> filePaths(String owner)
    {
        return new HashSet<>(store(owner).files.keySet());
    }

    Set<String> directoryPaths(String owner)
    {
        return new HashSet<>(store(owner).directories);
    }

    Set<String> readSources()
    {
        return new HashSet<>(readSources);
    }

    Set<String> fileMutationPaths()
    {
        return new HashSet<>(fileMutationPaths);
    }

    Set<String> copySources()
    {
        return new HashSet<>(copySources);
    }

    void clearOperations()
    {
        readSources.clear();
        fileMutationPaths.clear();
        copySources.clear();
    }

    byte[] content(String owner, String path)
    {
        byte[] content = store(owner).files.get(normalize(path));
        return content == null ? null : Arrays.copyOf(content, content.length);
    }

    @Override
    public File[] list(String owner, String source, Boolean recursively) throws Exception
    {
        if (busyListFailures > 0)
        {
            busyListFailures--;
            throw new IllegalArgumentException("\"exceptionCode\" : 10011, Path can't be operated by: List - " + owner
                    + " is currently being used");
        }
        Store store = store(owner);
        String path = normalize(source);
        if (store.files.containsKey(path))
        {
            return new File[] { toFile(owner, path, false, store.files.get(path).length) };
        }
        if (store.directories.contains(path) == false)
        {
            throw missing(path);
        }

        List<File> result = new ArrayList<>();
        for (String directory : store.directories)
        {
            if (directory.equals(path) == false && isListed(path, directory, recursively))
            {
                result.add(toFile(owner, directory, true, null));
            }
        }
        for (Map.Entry<String, byte[]> entry : store.files.entrySet())
        {
            if (isListed(path, entry.getKey(), recursively))
            {
                result.add(toFile(owner, entry.getKey(), false, entry.getValue().length));
            }
        }
        result.sort(Comparator.comparing(File::getPath));
        return result.toArray(File[]::new);
    }

    @Override
    public Chunk[] read(Chunk[] chunks) throws Exception
    {
        if (failReads)
        {
            throw new Exception("Injected read failure");
        }
        Chunk[] result = new Chunk[chunks.length];
        for (int i = 0; i < chunks.length; i++)
        {
            Chunk chunk = chunks[i];
            readSources.add(normalize(chunk.getSource()));
            byte[] content = requiredContent(chunk.getOwner(), chunk.getSource());
            int from = Math.toIntExact(chunk.getOffset());
            int to = Math.min(from + chunk.getLimit(), content.length);
            result[i] = chunk.toBuilder().data(Arrays.copyOfRange(content, from, to)).build();
        }
        return result;
    }

    @Override
    public Boolean write(Chunk[] chunks)
    {
        for (Chunk chunk : chunks)
        {
            Store store = store(chunk.getOwner());
            String path = normalize(chunk.getSource());
            fileMutationPaths.add(path);
            byte[] previous = store.files.getOrDefault(path, new byte[0]);
            int requiredLength = Math.toIntExact(chunk.getOffset()) + chunk.getData().length;
            byte[] content = Arrays.copyOf(previous, Math.max(previous.length, requiredLength));
            System.arraycopy(chunk.getData(), 0, content, Math.toIntExact(chunk.getOffset()), chunk.getData().length);
            addParentDirectories(store, path);
            store.files.put(path, content);
        }
        return true;
    }

    @Override
    public Boolean delete(String owner, String source, Boolean trash) throws Exception
    {
        String path = normalize(source);
        Store store = store(owner);
        store.files.keySet().stream().filter(candidate -> isSameOrDescendant(path, candidate))
                .forEach(fileMutationPaths::add);
        if (trash)
        {
            move(owner, path, owner, TRASH_PATH + path);
        } else
        {
            remove(store(owner), path);
        }
        return true;
    }

    @Override
    public Boolean copy(String sourceOwner, String source, String targetOwner, String target) throws Exception
    {
        copySources.add(normalize(source));
        fileMutationPaths.add(normalize(target));
        addFile(targetOwner, target, requiredContent(sourceOwner, source));
        return true;
    }

    @Override
    public Boolean move(String sourceOwner, String source, String targetOwner, String target) throws Exception
    {
        Store sourceStore = store(sourceOwner);
        Store targetStore = store(targetOwner);
        String sourcePath = normalize(source);
        String targetPath = normalize(target);
        if (exists(targetStore, targetPath))
        {
            throw new IllegalStateException("Target exists: " + targetPath);
        }
        if (exists(sourceStore, sourcePath) == false)
        {
            throw missing(sourcePath);
        }

        String sourceSnapshots = snapshotsDirectoryFor(sourcePath);
        if (exists(sourceStore, sourceSnapshots))
        {
            movePath(sourceStore, targetStore, sourceSnapshots, snapshotsDirectoryFor(targetPath));
        }
        movePath(sourceStore, targetStore, sourcePath, targetPath);
        return true;
    }

    @Override
    public Boolean create(String owner, String source, Boolean directory)
    {
        Store store = store(owner);
        String path = normalize(source);
        addParentDirectories(store, path);
        if (directory)
        {
            store.directories.add(path);
        } else
        {
            store.files.put(path, new byte[0]);
        }
        return true;
    }

    @Override
    public Boolean truncate(String owner, String source, Long size) throws Exception
    {
        Store store = store(owner);
        String path = normalize(source);
        byte[] content = requiredContent(owner, path);
        store.files.put(path, Arrays.copyOf(content, Math.toIntExact(size)));
        return true;
    }

    @Override
    public Boolean snapshot(String owner, String source) throws Exception
    {
        String name = String.format("2026_01_01_00_00_00_%03d", snapshotNumber++);
        addFile(owner, snapshotsDirectoryFor(source) + "/" + name, requiredContent(owner, source));
        return true;
    }

    @Override
    public String hash(String owner, String source) throws Exception
    {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(requiredContent(owner, source)));
    }

    @Override
    public FreeSpace free(String owner, String source)
    {
        return new FreeSpace(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Override
    public byte[] preview(String owner, String source) throws Exception
    {
        return requiredContent(owner, source);
    }

    @Override
    public Object status(UUID operationId)
    {
        return null;
    }

    @Override
    public String login(String userId, String password)
    {
        return "session";
    }

    @Override
    public Boolean isSessionValid()
    {
        return true;
    }

    @Override
    public Boolean logout()
    {
        return true;
    }

    @Override
    public void begin(UUID transactionId)
    {
    }

    @Override
    public Boolean prepare()
    {
        return true;
    }

    @Override
    public void commit()
    {
    }

    @Override
    public void rollback()
    {
    }

    @Override
    public List<UUID> recover()
    {
        return List.of();
    }

    private void movePath(Store sourceStore, Store targetStore, String source, String target)
    {
        Map<String, byte[]> movedFiles = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : sourceStore.files.entrySet())
        {
            if (isSameOrDescendant(source, entry.getKey()))
            {
                String targetPath = replacePrefix(source, target, entry.getKey());
                fileMutationPaths.add(entry.getKey());
                fileMutationPaths.add(targetPath);
                movedFiles.put(targetPath, entry.getValue());
            }
        }
        Set<String> movedDirectories = new HashSet<>();
        for (String directory : sourceStore.directories)
        {
            if (isSameOrDescendant(source, directory))
            {
                movedDirectories.add(replacePrefix(source, target, directory));
            }
        }

        remove(sourceStore, source);
        addParentDirectories(targetStore, target);
        targetStore.files.putAll(movedFiles);
        targetStore.directories.addAll(movedDirectories);
    }

    private void remove(Store store, String path)
    {
        store.files.keySet().removeIf(candidate -> isSameOrDescendant(path, candidate));
        store.directories.removeIf(candidate -> candidate.equals("/") == false && isSameOrDescendant(path, candidate));
        if (isInSnapshots(path) == false)
        {
            String snapshots = snapshotsDirectoryFor(path);
            store.files.keySet().removeIf(candidate -> isSameOrDescendant(snapshots, candidate));
            store.directories.removeIf(candidate -> isSameOrDescendant(snapshots, candidate));
        }
    }

    private byte[] requiredContent(String owner, String path) throws Exception
    {
        byte[] content = store(owner).files.get(normalize(path));
        if (content == null)
        {
            throw missing(path);
        }
        return content;
    }

    private Store store(String owner)
    {
        return stores.computeIfAbsent(owner, ignored -> new Store());
    }

    private static Exception missing(String path)
    {
        return new Exception("NoSuchFileException: " + path);
    }

    private static boolean exists(Store store, String path)
    {
        return store.files.containsKey(path) || store.directories.contains(path);
    }

    private static boolean isListed(String source, String candidate, boolean recursively)
    {
        if (isSameOrDescendant(source, candidate) == false || source.equals(candidate))
        {
            return false;
        }
        return recursively || parent(candidate).equals(source);
    }

    private static boolean isSameOrDescendant(String parent, String candidate)
    {
        return candidate.equals(parent) || candidate.startsWith(parent.equals("/") ? "/" : parent + "/");
    }

    private static String replacePrefix(String source, String target, String path)
    {
        return target + path.substring(source.length());
    }

    private static void addParentDirectories(Store store, String path)
    {
        String current = parent(path);
        while (current.isEmpty() == false)
        {
            store.directories.add(current);
            if (current.equals("/"))
            {
                return;
            }
            current = parent(current);
        }
    }

    private static File toFile(String owner, String path, boolean directory, Integer size)
    {
        return new File(owner, path, name(path), directory, size == null ? null : size.longValue(), LAST_MODIFIED);
    }

    private static String snapshotsDirectoryFor(String path)
    {
        String parent = parent(path);
        String prefix = parent.equals("/") ? "" : parent;
        return prefix + "/" + SNAPSHOTS_DIRECTORY + "/" + name(path);
    }

    private static boolean isInSnapshots(String path)
    {
        return path.contains("/" + SNAPSHOTS_DIRECTORY + "/");
    }

    private static String normalize(String path)
    {
        if (path.equals("/"))
        {
            return path;
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static String parent(String path)
    {
        int slash = path.lastIndexOf('/');
        return slash <= 0 ? "/" : path.substring(0, slash);
    }

    private static String name(String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static final class Store
    {
        private final Map<String, byte[]> files = new HashMap<>();

        private final Set<String> directories = new HashSet<>(Set.of("/"));
    }
}
