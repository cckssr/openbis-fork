package ch.systemsx.cisd.openbis.generic.server.codeplugin;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import ch.systemsx.cisd.common.properties.PropertyUtils;

public class CodePluginsConfiguration
{

    public static final String ENABLED_PROPERTY = "code-plugins.enabled";

    public static final boolean ENABLED_DEFAULT = true;

    public static final String ALLOWED_EDITING_USERS_PROPERTY = "code-plugins.allowed-editing-users";

    public static final String ALLOWED_USERS_DEFAULT = "system";

    private final boolean enabled;

    private final List<String> allowedUsers;

    public CodePluginsConfiguration(Properties properties)
    {
        this.enabled = PropertyUtils.getBoolean(properties, ENABLED_PROPERTY, ENABLED_DEFAULT);

        List<String> allowedUsers = PropertyUtils.tryGetListInOriginalCase(properties, ALLOWED_EDITING_USERS_PROPERTY);
        if (allowedUsers == null)
        {
            allowedUsers = Collections.singletonList(ALLOWED_USERS_DEFAULT);
        }

        this.allowedUsers = allowedUsers;
    }

    public boolean areEnabled()
    {
        return enabled;
    }

    public boolean isAllowedUser(String userId)
    {
        if (userId == null)
        {
            throw new IllegalArgumentException("User id cannot be null");
        }

        for (String allowedUser : allowedUsers)
        {
            if (userId.matches(allowedUser))
            {
                return true;
            }
        }

        return false;
    }

    public void checkEnabled()
    {
        if (!areEnabled())
        {
            throw new CodePluginsDisabledException();
        }
    }

    public void checkAllowedUser(String userId)
    {
        if (!isAllowedUser(userId))
        {
            throw new CodePluginsConfigurationNotAllowedException(userId);
        }
    }

}
