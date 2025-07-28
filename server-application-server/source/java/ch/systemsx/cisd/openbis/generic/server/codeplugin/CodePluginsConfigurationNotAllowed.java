package ch.systemsx.cisd.openbis.generic.server.codeplugin;

import ch.systemsx.cisd.openbis.generic.client.web.client.exception.UserFailureException;

public class CodePluginsConfigurationNotAllowed extends UserFailureException
{
    public CodePluginsConfigurationNotAllowed(final String userId)
    {
        super("User '" + userId + "' is not allowed to change code plugins configuration. Use '" + CodePluginsConfiguration.ALLOWED_USERS
                + "' property in AS service.properties to change the list of allowed users.");
    }
}
