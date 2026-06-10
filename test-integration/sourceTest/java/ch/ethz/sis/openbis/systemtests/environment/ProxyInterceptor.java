package ch.ethz.sis.openbis.systemtests.environment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ProxyInterceptor
{

    void invoke(HttpServletRequest request, HttpServletResponse response, String method, DefaultAction defaultAction) throws Exception;

    interface DefaultAction {

        void execute(HttpServletRequest request, HttpServletResponse response) throws Exception;

    }

}
