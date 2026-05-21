package org.apache.logging.log4j.util;

public final class Unbox
{
    private Unbox()
    {
    }

    public static StringBuilder box(byte value)
    {
        return new StringBuilder().append(value);
    }

    public static StringBuilder box(short value)
    {
        return new StringBuilder().append(value);
    }

    public static StringBuilder box(int value)
    {
        return new StringBuilder().append(value);
    }

    public static StringBuilder box(long value)
    {
        return new StringBuilder().append(value);
    }

    public static StringBuilder box(boolean value)
    {
        return new StringBuilder().append(value);
    }
}
