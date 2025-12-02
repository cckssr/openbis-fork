package ch.ethz.sis.rocrateserver.openapi.v1.service.delegates;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.AsyncJobRegistry;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ExportJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;

@ApplicationScoped
public class ExportDelegate
{

    public AsyncJob export(
            AsyncJobRegistry asyncJobRegistry,
            OpenBIS openBIS,
            ExportParams headers,
            InputStream body) throws Exception
    {
        String userName = openBIS.getSessionInformation().getUserName();
        ExportJob exportJob = new ExportJob(headers, body, openBIS, userName);
        String jobId = asyncJobRegistry.register(exportJob);

        return new AsyncJob(jobId);

    }
}
