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

package ch.ethz.sis.shared.log.standard.handlers;

import java.io.Writer;
import java.util.logging.*;
import java.io.IOException;

public class CustomWriterHandler extends Handler {
    private final Writer write;

    public CustomWriterHandler(Writer write) {
        this.write = write;
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String message = getFormatter().format(record);
        try {
            write.write(message.toCharArray(), 0, message.length());
            write.flush();
        } catch (IOException e) {
            reportError("Error writing log record", e, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public void flush() {
        try {
            write.flush();
        } catch (IOException e) {
            reportError("Error flushing log handler", e, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            write.close();
        } catch (IOException e) {
            reportError("Error closing log handler", e, ErrorManager.CLOSE_FAILURE);
        }
    }
}
