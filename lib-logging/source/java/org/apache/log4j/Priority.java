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

package org.apache.log4j;

/**
 * Minimal drop‐in replacement for log4j's Priority.
 */
/**
 * @deprecated This class is deprecated.
 *             Please use {@code ch.ethz.sis.shared.log.standard} classes instead.
 *
 * <p>This class is kept only for compatibility with some external libraries
 * used in openBIS. These libraries still expect the old Log4j 1.x
 * {@code Priority} class. They may try to access
 * {@code org.apache.log4j.Priority} directly or through reflection.</p>
 *
 * <p>To avoid runtime errors, we keep this simple class while openBIS
 * moves to the new logging system.</p>
 */

@Deprecated
public class Priority {
    protected int level;
    protected String levelStr;

    protected Priority(int level, String levelStr) {
        this.level = level;
        this.levelStr = levelStr;
    }

    public int toInt() {
        return level;
    }

    @Override
    public String toString() {
        return levelStr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Priority)) return false;
        Priority other = (Priority) o;
        return this.level == other.level && this.levelStr.equals(other.levelStr);
    }

    @Override
    public int hashCode() {
        return level;
    }
}
