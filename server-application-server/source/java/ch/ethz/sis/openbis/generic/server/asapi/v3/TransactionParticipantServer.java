package ch.ethz.sis.openbis.generic.server.asapi.v3;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionParticipantApi;
import ch.systemsx.cisd.openbis.common.api.server.AbstractApiServiceExporter;

@Controller
public class TransactionParticipantServer extends AbstractApiServiceExporter
{

    @Autowired
    private ITransactionParticipantApi transactionParticipantApi;

    @Override
    public void afterPropertiesSet()
    {
        establishService(ITransactionParticipantApi.class, transactionParticipantApi, ITransactionParticipantApi.SERVICE_NAME,
                ITransactionParticipantApi.SERVICE_URL);
        super.afterPropertiesSet();
    }

    @RequestMapping(
            { ITransactionParticipantApi.SERVICE_URL, "/openbis" + ITransactionParticipantApi.SERVICE_URL,
                    "/openbis/openbis" + ITransactionParticipantApi.SERVICE_URL })
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        super.handleRequest(request, response);
    }
}
