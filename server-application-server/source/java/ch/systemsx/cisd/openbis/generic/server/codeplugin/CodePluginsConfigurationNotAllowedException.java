package ch.systemsx.cisd.openbis.generic.server.codeplugin;

import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class CodePluginsConfigurationNotAllowedException extends UserFailureException
{
    public CodePluginsConfigurationNotAllowedException(final String userId)
    {
        super("User '" + userId + "' is not allowed to change code plugins configuration. Use '" + CodePluginsConfiguration.ALLOWED_EDITING_USERS_PROPERTY
                + "' property in AS service.properties to change the list of allowed users.");
    }
}
