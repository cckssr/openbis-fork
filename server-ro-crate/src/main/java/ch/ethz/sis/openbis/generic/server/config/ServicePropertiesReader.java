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

package ch.ethz.sis.openbis.generic.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class ServicePropertiesReader {

    private final Properties props = new Properties();

    public ServicePropertiesReader(File path) {
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load service properties from " + path, e);
        }
    }

    public String getHttpServerUri() {
        return props.getProperty("httpServerUri");
    }

    public int getHttpServerPort() {
        return Integer.parseInt(props.getProperty("httpServerPort"));
    }

    public String getOpenBISUrl() {
        return props.getProperty("openBISUrl");
    }

    public int getOpenBISTimeout() {
        return Integer.parseInt(props.getProperty("openBISTimeout"));
    }

    public String getOpenBISUser() {
        return props.getProperty("openBISUser");
    }

    public String getOpenBISPassword() {
        return props.getProperty("openBISPassword");
    }

    // Optionally, expose the raw Properties object
    public Properties asProperties() {
        return props;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName())
                .append(" [\n");
        props.forEach((key, val) ->
                sb.append("  ")
                        .append(key)
                        .append(" = ")
                        .append(val)
                        .append("\n")
        );
        sb.append("]");
        return sb.toString();
    }
}
