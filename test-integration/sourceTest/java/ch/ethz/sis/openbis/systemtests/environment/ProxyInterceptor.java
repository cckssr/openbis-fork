package ch.ethz.sis.openbis.systemtests.environment;

import java.util.concurrent.Callable;

public interface ProxyInterceptor
{

    void invoke(String method, Callable<Void> defaultAction) throws Exception;

}
