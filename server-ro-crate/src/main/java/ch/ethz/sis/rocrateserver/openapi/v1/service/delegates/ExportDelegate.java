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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExportDelegate
{
    public static Set<String> ACCEPTABLE_MIME_TYPES =
            Set.of(RoCrateService.APPLICATION_LD_JSON, RoCrateService.APPLICATION_ZIP);

    public AsyncJob export(
            AsyncJobRegistry asyncJobRegistry,
            OpenBIS openBIS,
            ExportParams headers,
            InputStream body) throws Exception
    {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String exportMimeType = headers.getExportMimeType();
        if (!ACCEPTABLE_MIME_TYPES.contains(exportMimeType))
        {
            throw new WebApplicationException(
                    exportMimeType + " is not spported, please use" + ACCEPTABLE_MIME_TYPES.stream()
                            .sorted().collect(
                                    Collectors.joining(",")), Response.Status.NOT_ACCEPTABLE);
        }
        // Code simulating the copy
        // You could alternatively use NIO
        // And please, unlike me, do something about the Exceptions :D
        byte[] buffer = new byte[1024];
        int len;
        while ((len = body.read(buffer)) > -1)
        {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        // Open new InputStreams using recorded bytes
        // Can be repeated as many times as you wish
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());



        String userName = openBIS.getSessionInformation().getUserName();
        ExportJob exportJob =
                new ExportJob(Clock.systemUTC(), headers, inputStream, openBIS, userName
                );
        String jobId = asyncJobRegistry.register(exportJob);

        return new AsyncJob(jobId);

    }
}
