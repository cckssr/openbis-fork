package ch.ethz.sis.tools.referencecheck;

import junit.framework.TestCase;
import org.junit.Test;

public class OpenBisCheckTest extends TestCase
{

    private static final String URL = "http://localhost:8888";

    String token = "token";

    @Test
    public void testCheck()
    {

        String space = "MATERIALS";
        String project = "MOLECULES";
        String localName = "MOLE151";

        ReferenceCheck.StatementToFix statementToFix =
                new ReferenceCheck.StatementToFix("lol", "lmao", localName);
        ReferenceCheck.StatementToFix statementToFix2 =
                new ReferenceCheck.StatementToFix("lol", "lmao", "ASDFLJKA");

        /* Dummied out, this was a driver for development
        List<OpenBisCheck.CheckResult> checkResults =
                OpenBisCheck.checkResultList(URL, token, space, project,
                        List.of(statementToFix, statementToFix2));
        assertTrue(checkResults.get(0).found());
        assertFalse(checkResults.get(1).found());

         */

    }

}