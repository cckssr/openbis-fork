package ch.openbis.drive.model;

import ch.openbis.drive.util.OsDetectionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class SyncJobTest {

    @Test
    public void testDefaultValues() {
        SyncJob syncJob = new SyncJob(SyncJob.Type.Bidirectional, "https://url", "PAT", "entity-id", "/remote-root", "/home/user/local-dir", false);
        Assert.assertTrue(syncJob.isSkipHiddenFiles());
        Assert.assertEquals(SyncJob.getDefaultHiddenPathPatterns(), syncJob.getHiddenPathPatterns());
    }

    @Test
    public void testDefaultHiddenPathPatterns() {
        Assert.assertTrue(
            SyncJob.getDefaultHiddenPathPatterns().stream().allMatch(
            regex -> {
                try {
                    Pattern.compile(regex);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            })
        );

        Assert.assertTrue(SyncJob.getDefaultHiddenPathPatterns().containsAll(SyncJob.getDefaultHiddenPathPatternsForAnyPlatform()));
        Assert.assertTrue(SyncJob.getDefaultHiddenPathPatterns().containsAll(SyncJob.getDefaultHiddenPathPatternsForCurrentPlatform(OsDetectionUtil.detectOS())));
    }

    @Test
    public void testDefaultHiddenPathPatternsForSpecificPlatformPlatform() {
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Linux).containsAll(SyncJob.getDefaultHiddenPathPatternsForLinux())
        );
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Windows).containsAll(SyncJob.getDefaultHiddenPathPatternsForWindows())
        );
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Mac).containsAll(SyncJob.getDefaultHiddenPathPatternsForMacOS())
        );
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForCurrentPlatform(OsDetectionUtil.OS.Unknown).isEmpty()
        );
    }

    @Test
    public void testDefaultHiddenPathPatternsForAnyPlatform() {
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForAnyPlatform().stream().allMatch(
                        regex -> {
                            try {
                                Pattern.compile(regex);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForAnyPlatform().containsAll(
                    List.of(
                        //HIDDEN FILES WITH "DOT"
                        ".*[/\\\\]\\..*",
                        ".*'.*",
                        ".*~.*",
                        ".*\\$.*",
                        ".*%.*"
                    )
                )
        );
    }

    @Test
    public void testDefaultHiddenPathPatternsForLinux() {
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForLinux().stream().allMatch(
                        regex -> {
                            try {
                                Pattern.compile(regex);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForLinux().containsAll(
                    List.of(
                        //LINUX SYSTEM
                        "/bin(/.*)?",
                        "/boot(/.*)?",
                        "/dev(/.*)?",
                        "/etc(/.*)?",
                        "/lib(/.*)?",
                        "/media(/.*)?",
                        "/mnt(/.*)?",
                        "/opt(/.*)?",
                        "/proc(/.*)?",
                        "/root(/.*)?",
                        "/run(/.*)?",
                        "/sbin(/.*)?",
                        "/snap(/.*)?",
                        "/srv(/.*)?",
                        "/sys(/.*)?",
                        "/tmp(/.*)?",
                        "/usr(/.*)?",
                        "/var(/.*)?"
                    )
                )
        );
    }

    @Test
    public void testDefaultHiddenPathPatternsForWindows() {
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForWindows().stream().allMatch(
                        regex -> {
                            try {
                                Pattern.compile(regex);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForWindows().containsAll(
                    List.of(
                        //WINDOWS DB FILES
                        ".*[/\\\\]desktop\\.ini",
                        ".*[/\\\\]IconCache\\.db",
                        ".*[/\\\\]thumbs\\.db",

                        //WINDOWS SYSTEM
                        "([^:]+:)?[/\\\\]Windows([/\\\\].*)?",
                        "([^:]+:)?[/\\\\]Program Files([/\\\\].*)?",
                        "([^:]+:)?[/\\\\]Program Files \\(x86\\)([/\\\\].*)?",
                        "([^:]+:)?[/\\\\]ProgramData([/\\\\].*)?"
                    )
                )
        );
    }

    @Test
    public void testDefaultHiddenPathPatternsForMacOS() {
        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForMacOS().stream().allMatch(
                        regex -> {
                            try {
                                Pattern.compile(regex);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
        );

        Assert.assertTrue(
                SyncJob.getDefaultHiddenPathPatternsForMacOS().containsAll(
                    List.of(
                            //MAC-OS SYSTEM
                            "/Applications(/.*)?",
                            "/Library(/.*)?",
                            "/System(/.*)?",
                            "/Volumes(/.*)?"
                    )
                )
        );
    }
}
