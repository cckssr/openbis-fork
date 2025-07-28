package ch.systemsx.cisd.openbis.generic.server.codeplugin;

import java.util.List;
import java.util.Properties;

import ch.systemsx.cisd.common.properties.PropertyUtils;

public class CodePluginsConfiguration
{

    public static final String ENABLED_PROPERTY = "code-plugins.enabled";

    public static final boolean ENABLED_DEFAULT = true;

    public static final String ALLOWED_USERS = "code-plugins.allowed-users";

    private final boolean enabled;

    private final List<String> allowedUsers;

    public CodePluginsConfiguration(Properties properties)
    {
        enabled = PropertyUtils.getBoolean(properties, ENABLED_PROPERTY, ENABLED_DEFAULT);
        allowedUsers = PropertyUtils.getList(properties, ALLOWED_USERS);
    }

    public boolean areEnabled()
    {
        return enabled;
    }

    public boolean isAllowedUser(String userId)
    {
        return allowedUsers.contains(userId);
    }

    public void checkAllowedUser(String userId)
    {
        if (!isAllowedUser(userId))
        {
            throw new CodePluginsConfigurationNotAllowed(userId);
        }
    }

}
