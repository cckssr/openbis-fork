/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.dev;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created based on the openBISDevelopmentEnvironmentASStart config
 */
public class EmbeddedJettyDevelopmentServer
{

    public static void main(String[] args) throws Exception {
        List<String> classesDirs = new ArrayList<>();
        String libDir = null;
        int port = 0;
        String webappBase = null;

        // parse args exactly as “--classes DIR” … “--lib DIR” “--port N” “<webappBase>”
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--classes":
                    if (i + 1 >= args.length) {
                        System.err.println("Expected a directory after --classes");
                        System.exit(1);
                    }
                    classesDirs.add(args[++i]);
                    break;

                case "--lib":
                    if (i + 1 >= args.length) {
                        System.err.println("Expected a directory after --lib");
                        System.exit(1);
                    }
                    libDir = args[++i];
                    break;

                case "--port":
                    if (i + 1 >= args.length) {
                        System.err.println("Expected a port number after --port");
                        System.exit(1);
                    }
                    port = Integer.parseInt(args[++i]);
                    break;

                default:
                    // The very last “free” argument is the webapp resource base
                    if (webappBase == null) {
                        webappBase = args[i];
                    } else {
                        System.err.println("Unknown extra argument: " + args[i]);
                        System.exit(1);
                    }
                    break;
            }
        }

        if (libDir == null || port <= 0 || webappBase == null) {
            System.err.println("Usage: EmbeddedJettyServer "
                    + "--classes <dir> [--classes <dir> …] "
                    + "--lib <libDir> "
                    + "--port <port> "
                    + "<webappBase>");
            System.exit(1);
        }

        // Build a ClassLoader combining all “classes” folders + all JARs under “lib”
        ClassLoader webappCl = createWebappClassLoader(classesDirs, libDir);

        // Create Jetty Server on the given port
        Server server = new Server(port);

        // Create WebAppContext: root path “/” serving files under webappBase
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase(webappBase);
        context.setParentLoaderPriority(true);
        context.setClassLoader(webappCl);

        // --- BEGIN: ensure jetty-web.xml in WEB-INF is picked up ---
        // Provide the list of Configuration classes, including JettyWebXmlConfiguration:
        context.setConfigurationClasses(new String[] {
                "org.eclipse.jetty.webapp.WebInfConfiguration",
                "org.eclipse.jetty.webapp.WebXmlConfiguration",
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"
        });

        // --- END: this ensures WEB-INF/jetty-web.xml is processed ---

        // WEB-INF/web.xml, Jetty picks it up automatically.
        server.setHandler(context);

        System.out.println(">>> Embedded Jetty starting on port " + port);
        server.start();
        server.join();
    }

    private static ClassLoader createWebappClassLoader(List<String> classesDirs, String libDir)
            throws MalformedURLException
    {
        List<URL> urls = new ArrayList<>();

        // 1) Add each “classes” directory as URL
        for (String dir : classesDirs) {
            File f = new File(dir);
            if (!f.exists() || !f.isDirectory()) {
                System.err.println("WARNING: classes directory not found: " + dir);
            } else {
                urls.add(f.toURI().toURL());
            }
        }

        // 2) Add every JAR in the libDir
        File libFolder = new File(libDir);
        if (!libFolder.exists() || !libFolder.isDirectory()) {
            System.err.println("ERROR: lib folder missing: " + libDir);
            System.exit(1);
        }
        File[] jars = libFolder.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                urls.add(jar.toURI().toURL());
            }
        }

        // Parent classloader should be the current thread’s context (so Jetty classes remain visible)
        return new URLClassLoader(urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader());
    }
}
