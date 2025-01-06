package ch.eth.sis.rocrate.parser;

public interface IAttribute
{
    abstract String getHeaderName();

    abstract boolean isMandatory();

    abstract boolean isUpperCase();
}