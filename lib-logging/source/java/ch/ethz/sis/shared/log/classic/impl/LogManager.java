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

package ch.ethz.sis.shared.log.classic.impl;

public class LogManager {
    
    /**
     * Returns a Logger with the specified name.
     * 
     * @param name the name of the Logger.
     * @return a Logger instance.
     */
    public static Logger getLogger(String name) {
        return Logger.getLogger(name);
    }
    
    /**
     * Returns a Logger named after the given class.
     * 
     * @param clazz the Class object for which the Logger should be named.
     * @return a Logger instance.
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }


    /**
     * Returns a root Logger
     *
     * @return a Logger instance.
     */
    public static Logger getRootLogger() {
        return Logger.getRootLogger();
    }

}
