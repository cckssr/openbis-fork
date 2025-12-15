package ch.openbis.drive.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;

@RunWith(JUnit4.class)
public class GlobUtilTest {
    @Test
    public void testCompileIgnoredPathGlob() throws Exception{
        Assert.assertEquals(2, GlobUtil.compileIgnoredPathGlob("testglob").size());
        Assert.assertEquals(4, GlobUtil.compileIgnoredPathGlob("**/testglob").size());

        Exception exception = null;
        try {
            Assert.assertEquals(4, GlobUtil.compileIgnoredPathGlob("**/test{glob").size());
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNotNull(exception);

        Assert.assertTrue(GlobUtil.compileIgnoredPathGlob("testglob").stream().anyMatch(
                matcher -> matcher.matches(Path.of("testglob/aaa"))
        ));
        Assert.assertTrue(GlobUtil.compileIgnoredPathGlob("testglob").stream().anyMatch(
                matcher -> matcher.matches(Path.of("testglob"))
        ));
        Assert.assertTrue(GlobUtil.compileIgnoredPathGlob("**/testglob").stream().anyMatch(
                matcher -> matcher.matches(Path.of("testglob"))
        ));
        Assert.assertTrue(GlobUtil.compileIgnoredPathGlob("**/testglob").stream().anyMatch(
                matcher -> matcher.matches(Path.of("a/b/testglob"))
        ));
    }
}
