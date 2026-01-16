package ch.systemsx.cisd.openbis.generic.server.util;

import org.eclipse.jetty.deploy.ContextHandlerFactory;
import org.eclipse.jetty.deploy.StandardContextHandlerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Attributes;
import org.eclipse.jetty.util.component.Environment;

import java.nio.file.Path;
import java.util.Set;

public class OpenbisContextHandlerFactory implements ContextHandlerFactory
{
    private final ContextHandlerFactory delegate = new StandardContextHandlerFactory();

    @Override
    public ContextHandler newContextHandler(Server server,
            Environment environment,
            Path mainPath,
            Set<Path> otherPaths,
            Attributes deployAttributes) throws Exception
    {
        ContextHandler handler =
                delegate.newContextHandler(server, environment, mainPath, otherPaths, deployAttributes);

        // Allow symlink/alias resources
        handler.getAliasChecks().add(new SymlinkAllowedResourceAliasChecker(handler));
//        handler.setInitParameter("aliases", "true");
//        handler.setInitParameter("org.eclipse.jetty.ee10.servlet.Default.aliases", "true");
        return handler;
    }
}
