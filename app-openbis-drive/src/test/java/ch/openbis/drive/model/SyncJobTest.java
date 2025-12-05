package ch.openbis.drive.model;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.regex.Pattern;

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

        Assert.assertTrue(
            SyncJob.getDefaultHiddenPathPatterns().containsAll(
                List.of(
                    //LINUX HIDDEN FILES WITH "DOT"
                    ".*[/\\\\]\\..*",

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
                    "/var(/.*)?",

                    //WINDOWS SYSTEM
                    "([^:]+:)?[/\\\\]Windows([/\\\\].*)?",
                    "([^:]+:)?[/\\\\]Program Files([/\\\\].*)?",
                    "([^:]+:)?[/\\\\]Program Files \\(x86\\)([/\\\\].*)?",
                    "([^:]+:)?[/\\\\]ProgramData([/\\\\].*)?",

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
