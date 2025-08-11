package ch.openbis.drive.model;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

@RunWith(JUnit4.class)
public class SettingsTest extends TestCase {
    @Test
    public void testGettingSyncJobs() {
        Settings settings = new Settings(true, "it", 60, null);
        Assert.assertEquals(Collections.emptyList(), settings.getJobs());
    }
}