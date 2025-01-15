package ch.ethz.sis.afsserver.server.observer;

import java.util.Map;

public interface APICall
{

    String getMethod();

    Map<String, Object> getParams();

    Object executeDefault() throws Exception;

}