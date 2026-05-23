//package ch.systemsx.cisd.openbis.generic.server.util;
//
//import org.eclipse.jetty.deploy.App;
//import org.eclipse.jetty.deploy.providers.ContextProvider;
//import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;
//import org.eclipse.jetty.server.handler.ContextHandler;
//
//public class OpenbisWebAppProvider extends ContextProvider
//{
//    @Override
//    public ContextHandler createContextHandler(App app) throws Exception
//    {
//        ContextHandler handler = super.createContextHandler(app);
//
//        // Allow serving resources through symlinks (Jetty 12+)
//        handler.getAliasChecks().add(new SymlinkAllowedResourceAliasChecker());
//
//        // Optional: keep old DefaultServlet compat flags
//        handler.setInitParameter("aliases", "true");
//        handler.setInitParameter("org.eclipse.jetty.ee10.servlet.Default.aliases", "true");
//
//        return handler;
//    }
//}
