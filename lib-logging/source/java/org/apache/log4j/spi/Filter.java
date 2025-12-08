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

package org.apache.log4j.spi;

import java.util.logging.LogRecord;

/**
 * @deprecated This interface is deprecated.
 *             Please use {@code ch.ethz.sis.shared.log.standard} classes instead.
 *
 * <p>This interface is kept only for compatibility with some external libraries
 * used in openBIS. These libraries still expect the old Log4j 1.x
 * {@code Filter} interface from {@code org.apache.log4j.spi}. Some libraries
 * may access it directly or through reflection.</p>
 *
 * <p>To avoid runtime errors, we keep this simple interface while openBIS
 * moves to the new logging system.</p>
 */

@Deprecated
public interface Filter
{
    int ACCEPT = 1;

    int DENY = 2;

    int decide(LogRecord event);
}
