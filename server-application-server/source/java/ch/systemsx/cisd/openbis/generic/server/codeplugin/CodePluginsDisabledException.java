package ch.systemsx.cisd.openbis.generic.server.codeplugin;

import ch.systemsx.cisd.openbis.generic.client.web.client.exception.UserFailureException;

public class CodePluginsDisabledException extends UserFailureException
{

    public CodePluginsDisabledException()
    {
        super("Code plugins are disabled. Use '" + CodePluginsConfiguration.ENABLED_PROPERTY + "' property in AS service.properties to enable them.");
    }

}
