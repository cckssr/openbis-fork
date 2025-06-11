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


package ch.ethz.sis.openbis.generic.server;

import ch.ethz.sis.openbis.generic.server.config.ServicePropertiesReader;
import ch.systemsx.cisd.common.logging.LogInitializer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class RoCrateApplication
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar lib-ro-crate.jar <path to service.properties>");
            System.exit(1);
        }

        LogInitializer.init();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        File cfg = new File(args[0]);
        System.out.println("Configuration Location: " + cfg.getCanonicalPath());
        ServicePropertiesReader reader = new ServicePropertiesReader(cfg);

        String contextPath = reader.getHttpServerUri();
        int port = reader.getHttpServerPort();

        System.out.println("→ starting on port " + port + " with context-path " + contextPath);

        SpringApplication app = createSpringApplication(port, contextPath, reader);
        app.run(args);
    }

    private static SpringApplication createSpringApplication(int port, String contextPath,
            ServicePropertiesReader reader)
    {
        Map<String, Object> springProps = new HashMap<>();
        springProps.put("server.port", port);
        springProps.put("server.servlet.context-path", contextPath);

        // 3) Launch Spring with those defaults
        SpringApplication app = new SpringApplication(RoCrateApplication.class);
        app.addInitializers((ConfigurableApplicationContext ctx) -> {
            ctx.getBeanFactory()
                    .registerSingleton("servicePropertiesReader", reader);
        });

        app.addListeners((ApplicationListener<ApplicationStartedEvent>) event -> {
            LogInitializer.init();
            System.out.println("Server started!");
        });

                app.setDefaultProperties(springProps);
        return app;
    }
}
