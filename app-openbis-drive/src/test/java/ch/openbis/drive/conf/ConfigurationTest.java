package ch.openbis.drive.conf;

import ch.openbis.drive.util.OpenBISDriveUtil;
import ch.openbis.drive.util.OsDetectionUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class ConfigurationTest extends TestCase {

    @Test
    public void testGetLocalAppDirectory() throws IOException {
        Configuration configuration = new Configuration(Path.of("/testpath/subdir"), 12345);
        Assert.assertEquals(Path.of("/testpath/subdir/launch-scripts"), configuration.getLocalAppLaunchDirectory());
        Assert.assertEquals(Path.of("/testpath/subdir/state"), configuration.getLocalAppStateDirectory());
        Assert.assertEquals(12345, configuration.getOpenbisDrivePort());
    }

    @Test()
    public void testGetLocalAppDirectoriesWithUserHome() throws IOException {
        String dirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
        System.setProperty("user.home", dirPath);
        for(String osDescription : List.of("linux", "win", "mac")) {
            System.setProperty("os.name", osDescription);
            OsDetectionUtil.OS expectedOS = switch (osDescription) {
                case "linux" -> OsDetectionUtil.OS.Linux;
                case "win" -> OsDetectionUtil.OS.Windows;
                case "mac" -> OsDetectionUtil.OS.Mac;
                default -> throw new IllegalArgumentException();
            };
            Configuration configuration = new Configuration();
            Assert.assertEquals(OpenBISDriveUtil.getLocalHiddenDirectoryPath(dirPath, expectedOS).toAbsolutePath().resolve(Configuration.LOCAL_OPENBIS_LAUNCH_SCRIPTS_DIRECTORY), configuration.getLocalAppLaunchDirectory());
            Assert.assertEquals(OpenBISDriveUtil.getLocalHiddenDirectoryPath(dirPath, expectedOS).toAbsolutePath().resolve(Configuration.LOCAL_OPENBIS_STATE_DIRECTORY), configuration.getLocalAppStateDirectory());
        }
    }

    @Test()
    public void testGetLocalAppDirectoriesWithEnv() throws IOException {
        String dirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
        Configuration configuration = new Configuration(Map.of(Configuration.LOCAL_APPLICATION_DIRECTORY_ENV_KEY, dirPath));
        Assert.assertEquals(Path.of(dirPath).toAbsolutePath().resolve(Configuration.LOCAL_OPENBIS_STATE_DIRECTORY), configuration.getLocalAppStateDirectory());
        Assert.assertEquals(Path.of(dirPath).toAbsolutePath().resolve(Configuration.LOCAL_OPENBIS_LAUNCH_SCRIPTS_DIRECTORY), configuration.getLocalAppLaunchDirectory());
    }
}