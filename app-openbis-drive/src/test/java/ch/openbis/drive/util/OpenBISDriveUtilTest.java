package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.util.List;

@RunWith(JUnit4.class)
public class OpenBISDriveUtilTest extends TestCase {

    @Test
    public void testGetLocalHiddenDirectoryPath() {
        for(OsDetectionUtil.OS os : OsDetectionUtil.OS.values()) {
            Path hiddenAppDirectoryPath = null;
            Exception exception = null;
            try {
                hiddenAppDirectoryPath = OpenBISDriveUtil.getLocalHiddenDirectoryPath(Path.of("dir", "sub", "dir").toString(), os);
            } catch (Exception e) {
                exception = e;
            }
            switch (os) {
                case Linux -> {
                    Assert.assertEquals(Path.of("dir", "sub", "dir", ".local", "state", Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), hiddenAppDirectoryPath);
                }
                case Windows -> {
                    Assert.assertEquals(Path.of("dir", "sub", "dir", "AppData", "Local", Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), hiddenAppDirectoryPath);
                }
                case Mac -> {
                    Assert.assertEquals(Path.of("dir", "sub", "dir", "Library", "Application Support", Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), hiddenAppDirectoryPath);
                }
                case Unknown -> {
                    Assert.assertTrue(exception instanceof IllegalArgumentException);
                }
            }
        }
    }

    @Test
    public void testTestGetLocalHiddenDirectoryPath() {
        for(OsDetectionUtil.OS os : OsDetectionUtil.OS.values()) {
            Path hiddenAppDirectoryPath = null;
            Exception exception = null;
            try {
                hiddenAppDirectoryPath = OpenBISDriveUtil.getLocalHiddenDirectoryPath(Path.of("dir", "sub", "dir"), os);
            } catch (Exception e) {
                exception = e;
            }
            switch (os) {
                case Linux -> {
                    Assert.assertEquals(Path.of("dir", "sub", "dir", ".local", "state", Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), hiddenAppDirectoryPath);
                }
                case Windows -> {
                    Assert.assertEquals(Path.of("dir", "sub", "dir", "AppData", "Local", Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), hiddenAppDirectoryPath);
                }
                case Mac -> {
                    Assert.assertEquals(Path.of("dir", "sub", "dir", "Library", "Application Support", Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), hiddenAppDirectoryPath);
                }
                case Unknown -> {
                    Assert.assertTrue(exception instanceof IllegalArgumentException);
                }
            }
        }
    }
}