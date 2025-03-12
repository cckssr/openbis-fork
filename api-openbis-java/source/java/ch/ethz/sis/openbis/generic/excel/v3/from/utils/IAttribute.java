package ch.ethz.sis.openbis.generic.excel.v3.from.utils;

public interface IAttribute
{
    abstract String getHeaderName();

    abstract boolean isMandatory();

    abstract boolean isUpperCase();
}