package ch.openbis.drive.model;

import ch.openbis.drive.DriveTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class SettingsTest extends DriveTestCase {
    @Test
    public void testGettingSyncJobs() {
        Settings settings = new Settings(true, "it", 60, null, new ArrayList<>(List.of("aaa", "bbb")));
        Assert.assertEquals(Collections.emptyList(), settings.getJobs());
    }
}