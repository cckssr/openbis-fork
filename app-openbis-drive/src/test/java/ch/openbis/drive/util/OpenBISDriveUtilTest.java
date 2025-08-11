package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;

@RunWith(JUnit4.class)
public class OpenBISDriveUtilTest extends TestCase {

    @Test
    public void testGetLocalHiddenDirectoryPath() {
        Assert.assertEquals(Path.of("/dir/sub/dir/" + Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), OpenBISDriveUtil.getLocalHiddenDirectoryPath("/dir/sub/dir"));
        Assert.assertEquals(Path.of("/dir/sub/dir/" + Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), OpenBISDriveUtil.getLocalHiddenDirectoryPath("/dir/sub/dir/"));
    }

    @Test
    public void testTestGetLocalHiddenDirectoryPath() {
        Assert.assertEquals(Path.of("/dir/sub/dir/" + Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), OpenBISDriveUtil.getLocalHiddenDirectoryPath(Path.of("/dir/sub/dir")));
        Assert.assertEquals(Path.of("/dir/sub/dir/" + Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY).toAbsolutePath(), OpenBISDriveUtil.getLocalHiddenDirectoryPath(Path.of("/dir/sub/dir/")));
    }
}