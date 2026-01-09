package ch.openbis.drive.model;

import ch.openbis.drive.util.OsDetectionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.FileSystems;
import java.util.List;

@RunWith(JUnit4.class)
public class SyncJobTest {

    @Test
    public void testDefaultValues() {
        SyncJob syncJob = new SyncJob(SyncJob.Type.Bidirectional, "https://url", "PAT", "entity-id", "title", "/remote-root", "/home/user/local-dir", false);
        Assert.assertEquals(SyncJob.IgnoredFilesMode.GlobalDefault, syncJob.getIgnoreFiles());
    }

    @Test
    public void testDefaultIgnoredPathPatterns() {
        Assert.assertTrue(
            SyncJob.getDefaultIgnoredPathPatterns().stream().allMatch(
            glob -> {
                try {
                    FileSystems.getDefault().getPathMatcher("glob:" + glob);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            })
        );

        Assert.assertTrue(SyncJob.getDefaultIgnoredPathPatterns().containsAll(SyncJob.getDefaultIgnoredPathPatternsForAnyPlatform()));
        Assert.assertTrue(SyncJob.getDefaultIgnoredPathPatterns().containsAll(SyncJob.getDefaultIgnoredPathPatternsForCurrentPlatform(OsDetectionUtil.detectOS())));
    }

    @Test
    public void testDefaultIgnoredPathPatternsForSpecificPlatformPlatform() {
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Linux).containsAll(SyncJob.getDefaultIgnoredPathPatternsForLinux())
        );
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Windows).containsAll(SyncJob.getDefaultIgnoredPathPatternsForWindows())
        );
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Mac).containsAll(SyncJob.getDefaultIgnoredPathPatternsForMacOS())
        );
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Unknown).isEmpty()
        );
    }

    @Test
    public void testDefaultIgnoredPathPatternsForAnyPlatform() {
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForAnyPlatform().stream().allMatch(
                        glob -> {
                            try {
                                FileSystems.getDefault().getPathMatcher("glob:" + glob);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForAnyPlatform().containsAll(
                    List.of(
                        //HIDDEN FILES WITH "DOT"
                        "**/.**",
                        "**'**",
                        "**~**",
                        "**$**",
                        "**%**"
                    )
                )
        );
    }

    @Test
    public void testDefaultIgnoredPathPatternsForLinux() {
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForLinux().stream().allMatch(
                        glob -> {
                            try {
                                FileSystems.getDefault().getPathMatcher("glob:" + glob);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForLinux().isEmpty()
        );
    }

    @Test
    public void testDefaultIgnoredPathPatternsForWindows() {
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForWindows().stream().allMatch(
                        glob -> {
                            try {
                                FileSystems.getDefault().getPathMatcher("glob:" + glob);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForWindows().containsAll(
                    List.of(
                        "**/desktop.ini",
                        "**/Desktop.ini",
                        "**/IconCache.db",
                        "**/thumbs.db",
                        "**/Thumbs.db"
                    )
                )
        );
    }

    @Test
    public void testDefaultIgnoredPathPatternsForMacOS() {
        Assert.assertTrue(
                SyncJob.getDefaultIgnoredPathPatternsForMacOS().stream().allMatch(
                        glob -> {
                            try {
                                FileSystems.getDefault().getPathMatcher("glob:" + glob);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
            SyncJob.getDefaultIgnoredPathPatternsForMacOS().containsAll(
                List.of(
                    //MAC desktop store
                    "**/.DS_Store",
                    "**/._.DS_Store",

                    //MAC time machine markers
                    "**/.com.apple.timemachine.supported-*"
                )
            )
        );
    }
}
