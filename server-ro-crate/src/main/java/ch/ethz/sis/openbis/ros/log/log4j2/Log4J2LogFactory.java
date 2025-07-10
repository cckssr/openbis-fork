/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.ros.log.log4j2;

import ch.ethz.sis.openbis.ros.log.LogFactory;
import ch.ethz.sis.openbis.ros.log.Logger;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.logging.LoggerDiagnostics;

import java.io.File;

public class Log4J2LogFactory implements LogFactory {

    @Override
    public <T> Logger getLogger(Class<T> clazz) {
        return Log4JLogger.getLog4JLogger(clazz);
    }

    @Override public Logger getLogger(String name)
    {
        return Log4JLogger.getLog4JLogger(name);
    }

    @Override
    public void configure(String pathToConfigurationFile) {
        if (pathToConfigurationFile != null && !pathToConfigurationFile.isBlank()) {
            try { //FileInputStream fis = new FileInputStream(pathToConfigurationFile)) {

                LogInitializer.configureFromFile(new File(pathToConfigurationFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LoggerDiagnostics.info("[Log4J2LogFactory] No configuration file provided, skipping initialization!");
        }
    }
}