package ch.openbis.drive.conf;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@RunWith(JUnit4.class)
public class ConfigurationTest extends TestCase {

    @Test
    public void testGetLocalAppDirectory() throws IOException {
        Configuration configuration = new Configuration(Path.of("/testpath/subdir"), 12345);
        Assert.assertEquals(Path.of("/testpath/subdir"), configuration.getLocalAppDirectory());
        Assert.assertEquals(12345, configuration.getOpenbisDrivePort());
    }

    @Test()
    public void testGetLocalAppDirectoryWithUserHome() throws IOException {
        String dirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
        System.setProperty("user.home", dirPath);
        Configuration configuration = new Configuration();
        Assert.assertEquals(Path.of(dirPath, ".openbis-drive").toAbsolutePath(), configuration.getLocalAppDirectory());
    }

    @Test()
    public void testGetLocalAppDirectoryWithEnv() throws IOException {
        String dirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
        Configuration configuration = new Configuration(Map.of(Configuration.LOCAL_APPLICATION_DIRECTORY_ENV_KEY, dirPath));
        Assert.assertEquals(Path.of(dirPath).toAbsolutePath(), configuration.getLocalAppDirectory());
    }
}