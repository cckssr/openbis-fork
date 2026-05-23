package ch.ethz.sis.openbis.generic.server.asapi.v3;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionCoordinatorApi;
import ch.systemsx.cisd.openbis.common.api.server.AbstractApiServiceExporter;

@Controller
public class TransactionCoordinatorServer extends AbstractApiServiceExporter
{

    @Autowired
    private ITransactionCoordinatorApi transactionCoordinatorApi;

    @Override
    public void afterPropertiesSet()
    {
        establishService(ITransactionCoordinatorApi.class, transactionCoordinatorApi, ITransactionCoordinatorApi.SERVICE_NAME,
                ITransactionCoordinatorApi.SERVICE_URL);
        super.afterPropertiesSet();
    }

    @RequestMapping(
            { ITransactionCoordinatorApi.SERVICE_URL, "/openbis" + ITransactionCoordinatorApi.SERVICE_URL,
                    "/openbis/openbis" + ITransactionCoordinatorApi.SERVICE_URL })
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        super.handleRequest(request, response);
    }
}
