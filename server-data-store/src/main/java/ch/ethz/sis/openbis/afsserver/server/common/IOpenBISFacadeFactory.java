package ch.ethz.sis.openbis.afsserver.server.common;

public interface IOpenBISFacadeFactory
{
    IOpenBISFacade createFacade(String openBISUrl, String openBISUser, String openBISPassword, Integer openBISTimeout);
}
