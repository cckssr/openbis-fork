package ch.openbis.rocrate.app.parser;

public interface IAttribute
{
    abstract String getHeaderName();

    abstract boolean isMandatory();

    abstract boolean isUpperCase();
}