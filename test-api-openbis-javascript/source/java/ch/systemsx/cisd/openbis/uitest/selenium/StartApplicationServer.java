package ch.systemsx.cisd.openbis.uitest.selenium;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;

public class StartApplicationServer {

    public static String go() throws Exception {
        final Object lock = new Object();

        Runnable r = () -> {
            Server server = new Server(10000);

            server.addEventListener(new LifeCycle.Listener() {
                @Override public void lifeCycleStarted(LifeCycle event) {
                    synchronized (lock) { lock.notifyAll(); }
                }
            });

            WebAppContext context = new WebAppContext();
            context.setContextPath("/");

            // prefer the WAR if present, otherwise run from exploded dir
            Path war = Path.of("targets/gradle/openbis-war/openbis.war");
            if (Files.exists(war)) {
                // Jetty 12 still supports setWar(String) for a WAR path
                context.setWar(war.toAbsolutePath().toString()); // see WebAppContext#setWar
            } else {
                // For an exploded webapp use setWarResource(Resource)
                Path webroot = Path.of("targets/www");
                try {
                    Resource base = context.newResource(webroot.toUri()); // ServletContextHandler#newResource
                    context.setWarResource(base);                 // WebAppContext#setWarResource
                    // If you have an explicit web.xml:
                    context.setDescriptor(webroot.resolve("WEB-INF/web.xml").toString());

                    // Allow serving files via symlinks inside this base (Jetty 12-style)
                    context.addAliasCheck(new SymlinkAllowedResourceAliasChecker(context, base));
                    // (Alternative: new AllowedResourceAliasChecker(context, base))
                } catch (Exception e) {
                    throw new RuntimeException("Cannot configure exploded webapp at " + webroot, e);
                }
            }

            server.setHandler(context);
            try {
                server.start();
                server.join();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();

        synchronized (lock) { lock.wait(); }
        System.out.println("SERVER START DETECTED");
        return "http://localhost:10000";
    }
}
