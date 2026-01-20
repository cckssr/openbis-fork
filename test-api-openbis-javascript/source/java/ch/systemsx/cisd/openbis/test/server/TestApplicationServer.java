/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.test.server;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.util.resource.Resource;

/**
 * @author anttil
 */
public class TestApplicationServer
{

    private int port;

    private String webXmlPath;

    private String rootPath;

    private String contextPath;

    private String dumpsPath;

    private boolean deamon;

    public String start() throws Exception
    {
        TestDatabase.restoreDumps(getDumpsPath());

        Server server = new Server(getPort());

        WebAppContext context = new WebAppContext();

        File war = new File("../../../targets/gradle/openbis-war/openbis.war");
        if (war.exists())
        {
            context.setWar(war.getAbsolutePath());
            context.setExtractWAR(true);
            context.setTempDirectory(new File(System.getProperty("jetty.home") + "/webapps"));
            context.addAliasCheck(new SymlinkAllowedResourceAliasChecker(context));
        } else
        {
            context.setDescriptor(getWebXmlPath());
            Path webroot = Path.of(getRootPath());
            Resource base = context.newResource(webroot.toUri()); // ServletContextHandler#newResource
            context.setBaseResource(base);
        }
        context.setContextPath(getContextPath());
        context.setParentLoaderPriority(true);

        server.setHandler(context);

        server.start();

        return "http://localhost:" + getPort();
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return port;
    }

    public void setWebXmlPath(String webXmlPath)
    {
        this.webXmlPath = webXmlPath;
    }

    public String getWebXmlPath()
    {
        return webXmlPath;
    }

    public void setRootPath(String rootPath)
    {
        this.rootPath = rootPath;
    }

    public String getRootPath()
    {
        return rootPath;
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public void setDumpsPath(String dumpsPath)
    {
        this.dumpsPath = dumpsPath;
    }

    public String getDumpsPath()
    {
        return dumpsPath;
    }

    public void setDeamon(boolean deamon)
    {
        this.deamon = deamon;
    }

    public boolean isDeamon()
    {
        return deamon;
    }

}
