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

package ch.systemsx.cisd.common.logging;


import ch.systemsx.cisd.common.logging.LogFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaLoggingSimpleTest
{

    public static void main(String[] args) {
        // Initialize logging from logging.properties using the alias.
        System.setProperty("log4j.configuration" ,"/tmp/logging.properties");
        LogInitializer.init();

        LogFactory.getRootLogger().info("Hello");
        LogFactory.getLogger(JavaLoggingSimpleTest.class).info("Hello World");


    }

    private static void simpleCall()
    {
        // Obtain a logger and log messages as usual.
        Logger logger = Logger.getLogger(JavaLoggingSimpleTest.class.getName());
        logger.setLevel(Level.FINEST);

        logger.log(Level.FINE, "Hello " + System.currentTimeMillis());
        logger.log(Level.FINE, "Hello World");
    }

}
