package org.apache.logging.log4j;

import org.apache.logging.log4j.util.Supplier;

public interface LogBuilder
{
    LogBuilder NO_OP = new LogBuilder()
    {
    };

    default LogBuilder withThrowable(Throwable throwable)
    {
        return this;
    }

    default void log(String message)
    {
    }

    default void log(String message, Object p0)
    {
    }

    default void log(String message, Object p0, Object p1)
    {
    }

    default void log(String message, Object p0, Object p1, Object p2)
    {
    }

    default void log(String message, Object p0, Object p1, Object p2, Object p3)
    {
    }

    default void log(String message, Supplier<?>... paramSuppliers)
    {
    }

    default void log(Supplier<?> messageSupplier)
    {
    }
}
