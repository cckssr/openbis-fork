package ch.systemsx.cisd.openbis.generic.server.codeplugin;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CodePluginsConfigurationTest
{

    @Test
    public void testNoValues()
    {
        CodePluginsConfiguration configuration = new CodePluginsConfiguration(new Properties());
        Assert.assertFalse(configuration.areEnabled());
        Assert.assertTrue(configuration.isAllowedUser("system"));
        Assert.assertFalse(configuration.isAllowedUser("test1"));
        Assert.assertFalse(configuration.isAllowedUser("test2"));
    }

    @Test
    public void testConcreteValues()
    {
        Properties properties = new Properties();
        properties.setProperty(CodePluginsConfiguration.ENABLED_PROPERTY, String.valueOf(false));
        properties.setProperty(CodePluginsConfiguration.ALLOWED_USERS_PROPERTY, "test1, test2");

        CodePluginsConfiguration configuration = new CodePluginsConfiguration(properties);
        Assert.assertFalse(configuration.areEnabled());
        Assert.assertTrue(configuration.isAllowedUser("test1"));
        Assert.assertTrue(configuration.isAllowedUser("test2"));
        Assert.assertFalse(configuration.isAllowedUser("TEST2"));
        Assert.assertFalse(configuration.isAllowedUser("test3"));
    }

    @Test
    public void testRegexpValues()
    {
        Properties properties = new Properties();
        properties.setProperty(CodePluginsConfiguration.ENABLED_PROPERTY, String.valueOf(false));
        properties.setProperty(CodePluginsConfiguration.ALLOWED_USERS_PROPERTY, "test.*, hello");

        CodePluginsConfiguration configuration = new CodePluginsConfiguration(properties);
        Assert.assertFalse(configuration.areEnabled());
        Assert.assertTrue(configuration.isAllowedUser("test1"));
        Assert.assertTrue(configuration.isAllowedUser("test2"));
        Assert.assertFalse(configuration.isAllowedUser("TEST2"));
        Assert.assertTrue(configuration.isAllowedUser("hello"));
        Assert.assertFalse(configuration.isAllowedUser("world"));
    }

}
