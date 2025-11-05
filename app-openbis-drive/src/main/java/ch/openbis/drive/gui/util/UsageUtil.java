package ch.openbis.drive.gui.util;

import ch.openbis.drive.model.SyncJob;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UsageUtil {
    private static final BooleanProperty loading = new SimpleBooleanProperty(false);
    private static volatile Data data = null;

    @SuppressWarnings("Ignore lombok-getter suggestion")
    public static Data getData() {
        return data;
    }

    public static BooleanProperty getLoadingProperty() {
        return loading;
    }

    public static synchronized void load(Node node) {
        if(!loading.getValue()) {
            loading.setValue(true);
            new Thread( () -> {
                try {
                    ServiceCallHandler.ServiceCallResult<List<SyncJob>> syncJobsResult = SharedContext.getContext().getServiceCallHandler(node).getSyncJobs();
                    if (syncJobsResult.isOk()) {
                        long totalSize = getTotalSpaceOnPartition();
                        long availableSpace = getAvailableSpaceOnPartition();
                        Map<String, Long> localDirUsedSpaceMap = getLocalDirUsedSpaceMap(syncJobsResult.getOk());
                        long totalLocalDirSpace = localDirUsedSpaceMap.values().stream()
                                .reduce(0L, Long::sum);

                        data = new Data(totalSize, availableSpace, totalLocalDirSpace, localDirUsedSpaceMap);
                        loading.setValue(false);
                    } else {
                        throw syncJobsResult.getErr();
                    }
                } catch (Exception e) {
                    System.err.println("Error measuring disk-usage");
                    e.printStackTrace();
                    data = null;
                    loading.setValue(false);
                }
            }).start();
        }
    }

    private static Map<String, Double> getLocalDirUsedPercentageMap(Map<String, Long> localDirUsedSpaceMap, long totalSize) {
        HashMap<String, Double> localDirUsedPercentageMap = new HashMap<>();
        localDirUsedSpaceMap.forEach( (locDir, usedSpace) -> {
            localDirUsedPercentageMap.put(locDir, Math.min(((double) usedSpace) / totalSize * 100, 100.0));
        });
        return localDirUsedPercentageMap;
    }

    static private Map<String, Long> getLocalDirUsedSpaceMap(List<SyncJob> syncJobs) throws IOException {
        HashMap<String, Long> localDirUsedSpaceMap = new HashMap<>();
        for (SyncJob syncJob : syncJobs) {
            String localDir = syncJob.getLocalDirectoryRoot();
            Path localDirPath = null;
            try {
                localDirPath = Path.of(localDir);
            } catch (Exception e) { System.err.printf("Error parsing local directory path %s%n", localDir); }
            long localDirSize;
            if(localDirPath != null && Files.isDirectory(localDirPath)) {
                localDirSize = getOccupiedSpaceByDirectory(localDirPath);
            } else {
                localDirSize = 0L;
            }
            localDirUsedSpaceMap.put(localDir, localDirSize);
        }
        return localDirUsedSpaceMap;
    }

    static long getOccupiedSpaceByDirectory(@NonNull Path directory) throws IOException {
        if (Files.exists(directory)) {
            if (Files.isDirectory(directory)) {
                AtomicLong totalSize = new AtomicLong(0L);
                Files.walkFileTree(directory, Collections.emptySet(), 256, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (Files.isRegularFile(file)) {
                            try {
                                totalSize.addAndGet(Files.size(file));
                            } catch (Exception e) {
                                System.err.printf("Error getting size of file %s%n", file.toString());
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
                return totalSize.get();
            } else if (Files.isRegularFile(directory, LinkOption.NOFOLLOW_LINKS)) {
                return Files.size(directory);
            } else {
                return 0L;
            }
        } else {
            return 0L;
        }
    }

    static long getTotalSpaceOnPartition() {
        return Path.of(System.getProperty("user.home")).getRoot().toFile().getTotalSpace();
    }

    static long getAvailableSpaceOnPartition() {
        return Path.of(System.getProperty("user.home")).getRoot().toFile().getUsableSpace();
    }

    @Value
    public static class Data {
        long totalSize;
        long availableSpace;
        long totalLocalDirSpace;
        Map<String, Long> localDirUsedSpaceMap;

        double availableSpacePercentage;
        Map<String, Double> localDirUsedPercentageMap;
        double totalLocalDirSpacePercentage;

        public Data(long totalSize, long availableSpace, long totalLocalDirSpace, Map<String, Long> localDirUsedSpaceMap) {
            this.totalSize = totalSize;
            this.availableSpace = availableSpace;
            this.totalLocalDirSpace = totalLocalDirSpace;
            this.localDirUsedSpaceMap = localDirUsedSpaceMap;

            this.availableSpacePercentage = Math.min(((double) availableSpace) / totalSize * 100, 100.0);
            this.localDirUsedPercentageMap = UsageUtil.getLocalDirUsedPercentageMap(localDirUsedSpaceMap, totalSize);
            this.totalLocalDirSpacePercentage = Math.min(((double) totalLocalDirSpace) / totalSize * 100, 100.0);
        }
    }
}
