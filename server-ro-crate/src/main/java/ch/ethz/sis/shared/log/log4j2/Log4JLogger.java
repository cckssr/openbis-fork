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
package ch.ethz.sis.shared.log.log4j2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

class Log4JLogger extends Logger implements ch.ethz.sis.shared.log.Logger
{
    private static final String ENTRY_MARKER = "Enter ";
    private static final String EXIT_MARKER = "Exit ";

    private final String FQCN;

    public Log4JLogger(String name) {
        super(name);
        FQCN = this.getClass().getName();
//        Arrays.stream(Logger.getLogger(INTERNAL_LOGGER
//        ).getHandlers()).forEach(this.logger::addHandler);
//        this.logger.setUseParentHandlers(false);

    }

    /**
     * Returns a Logger for the given name.
     */
    public static ch.ethz.sis.shared.log.Logger getLog4JLogger(String name) {
        return new Log4JLogger(name);
    }

    public static ch.ethz.sis.shared.log.Logger getLog4JLogger(Class<?> clazz) {
        return getLog4JLogger(clazz.getName());
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
//            this.log(FQCN,
//                    Level.TRACE,
//                    ENTRY_MARKER,
//                    entryMsg(message, args),
//                    ex);
        }
    }

    @Override
    public <R> R traceExit(R result)
    {
        if(this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.TRACE, exitMsg( result), null);
//            this.log(FQCN,
//                    Level.TRACE,
//                    EXIT_MARKER,
//                    exitMsg((String) null, result),
//                    (Throwable) null);
        }
        return result;
    }

    @Override
    public void catching(Throwable ex)
    {
        String message = (ex.getMessage() != null) ? ex.getMessage() : "";
        this.log(Level.ERROR, message, ex);
        //        this.logMessage(FQCN,
//                Level.ERROR,
//                CATCHING_MARKER,
//                catchingMsg(ex),
//                ex);
    }

    @Override
    public <T extends Throwable> T throwing(T ex)
    {
        this.log(Level.ERROR,  ex.getMessage(), ex);
//        this.logMessage(FQCN,
//                Level.ERROR,
//                THROWING_MARKER,
//                throwingMsg(ex),
//                ex);
        return ex;
    }

    public void debug(String message, Object... args)
    {
        if(this.getEffectiveLevel() == Level.DEBUG || this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.DEBUG, format(message, args), null);
//            this.logMessage(FQCN,
//                    Level.DEBUG,
//                    null,
//                    logger.getMessageFactory().newMessage(message, args),
//                    (Throwable) null);
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
//        this.logMessage(FQCN,
//                Level.INFO,
//                null,
//                logger.getMessageFactory().newMessage(message, args),
//                ex);
    }

    public void warn(String message, Object... args)
    {
        if(this.getEffectiveLevel() == Level.WARN || this.getEffectiveLevel() == Level.INFO || this.getEffectiveLevel() == Level.DEBUG || this.getEffectiveLevel() == Level.TRACE)
        {
            this.log(Level.WARN, format(message, args), null);
//            this.logMessage(FQCN,
//                    Level.WARN,
//                    null,
//                    logger.getMessageFactory().newMessage(message, args),
//                    (Throwable) null);
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
