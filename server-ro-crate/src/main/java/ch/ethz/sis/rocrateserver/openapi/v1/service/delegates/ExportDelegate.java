package ch.ethz.sis.rocrateserver.openapi.v1.service.delegates;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.openapi.v1.service.RoCrateService;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.AsyncJobRegistry;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ExportJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExportDelegate
{
    Set<String> acceptableMimeTypes =
            Set.of(RoCrateService.APPLICATION_LD_JSON, RoCrateService.APPLICATION_ZIP);

    public AsyncJob export(
            AsyncJobRegistry asyncJobRegistry,
            OpenBIS openBIS,
            ExportParams headers,
            InputStream body) throws Exception
    {
        String exportMimeType = headers.getExportMimeType();
        if (!acceptableMimeTypes.contains(exportMimeType))
        {
            throw new WebApplicationException(
                    exportMimeType + " is not spported, please use" + acceptableMimeTypes.stream()
                            .sorted().collect(
                                    Collectors.joining(",")), Response.Status.NOT_ACCEPTABLE);
        }


        String userName = openBIS.getSessionInformation().getUserName();
        ExportJob exportJob = new ExportJob(headers, body, openBIS, userName);
        String jobId = asyncJobRegistry.register(exportJob);

        return new AsyncJob(jobId);

    }
}
