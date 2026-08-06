package ch.systemsx.cisd.authentication;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

public class SessionTokenHashTest
{
    @Test
    public void testValidToken()
    {
        assertTrue(SessionTokenHash.isValid("admin-260806112658411x3430222000638FC258CDDC43AB023139"));
    }

    @Test
    public void testUsernameWithDashToken()
    {
        assertTrue(SessionTokenHash.isValid("user.with-multiple-dash-260806112658411x3430222000638FC258CDDC43AB023139"));
    }

    @Test
    public void testMissingRandomPart_fail()
    {
        assertFalse(SessionTokenHash.isValid("admin-260806112658411x"));
    }

    @Test
    public void testNonNumericPart_fail()
    {
        assertFalse(SessionTokenHash.isValid("admin-XX0806112658411x3430222000638FC258CDDC43AB023139"));
    }

    @Test
    public void testEmpty_fail()
    {
        assertFalse(SessionTokenHash.isValid(""));
    }

    @Test
    public void testNull_fail()
    {
        assertFalse(SessionTokenHash.isValid(null));
    }


}
