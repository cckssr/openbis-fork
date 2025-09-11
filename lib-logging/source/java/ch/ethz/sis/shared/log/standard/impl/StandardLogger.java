/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.shared.log.standard.impl;

import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.log.standard.core.Level;

class StandardLogger extends AbstractJulLogger implements Logger
{
    private static final String ENTRY_MARKER = "Enter ";
    private static final String EXIT_MARKER = "Exit ";


    public StandardLogger(String name) {
        super(name);
    }

    /**
     * Returns a Logger for the given name.
     */
    public static Logger getStandardLogger(String name) {
        return new StandardLogger(name);
    }

    public static Logger getStandardLogger(Class<?> clazz) {
        return getStandardLogger(clazz.getName());
    }

    @Override
    public void traceAccess(String message, Object... args)
    {
        if(this.getEffectiveLevel() == Level.TRACE)
        {
            traceAccess(message, null, args);
        }
    }

    @Override public void traceAccess(String message, Throwable ex, Object... args)
    {
        if(this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.TRACE, format(ENTRY_MARKER, message, args), ex);
        }
    }

    @Override
    public <R> R traceExit(R result)
    {
        if(this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.TRACE, exitMsg( result), null);
        }
        return result;
    }

    @Override
    public void catching(Throwable ex)
    {
        String message = (ex.getMessage() != null) ? ex.getMessage() : "";
        this.log(Level.ERROR, message, ex);
    }

    @Override
    public <T extends Throwable> T throwing(T ex)
    {
        this.log(Level.ERROR,  ex.getMessage(), ex);
        return ex;
    }

    public void debug(String message, Object... args)
    {
        if(this.getEffectiveLevel() == Level.DEBUG || this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.DEBUG, format(message, args), null);
        }
    }

    @Override
    public void info(String message, Object... args)
    {
        info(message, null, args);
    }

    @Override public void info(String message, Throwable ex, Object... args)
    {
        this.log(Level.INFO, format(message, args), null);
    }

     public void warn(String message, Object... args)
    {
        if(this.getEffectiveLevel() == Level.WARN || this.getEffectiveLevel() == Level.INFO || this.getEffectiveLevel() == Level.DEBUG || this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.WARN, format(message, args), null);
        }
    }
    private String exitMsg(Object result) {
        return "EXIT Returning " + result;
    }

    private String format(String message, Object... args) {
        return format(null, message,args);
    }

    private String format(String markers, String message, Object... args) {
        int count = args == null ? 0 : args.length;
        String finalMessage = markers != null ? markers : "";
        if (count == 0) {
            finalMessage +=  message;
        } else if (message != null) {
            finalMessage +=  String.format(message, args);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("params(");
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Object arg = args[i];
                sb.append(String.valueOf(arg));
            }
            sb.append(")");
            finalMessage +=  sb.toString();
        }
        return finalMessage;
    }
}
