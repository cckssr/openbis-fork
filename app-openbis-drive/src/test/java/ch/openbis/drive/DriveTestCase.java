package ch.openbis.drive;

import ch.openbis.drive.logging.Logging;
import junit.framework.TestCase;
import org.junit.Test;

public class DriveTestCase extends TestCase {
    static {
        Logging.initializeTestLogging();
    }

    @Test
    public void testInitialize() {}
}
