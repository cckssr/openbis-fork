package ch.ethz.sis.openbis.systemtests.environment;

import java.util.concurrent.Callable;

public interface ServerProxyInterceptor
{

    void invoke(String method, Callable<Void> defaultAction) throws Exception;

}
