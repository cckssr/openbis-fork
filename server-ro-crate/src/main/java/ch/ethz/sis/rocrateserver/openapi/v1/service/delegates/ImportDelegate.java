package ch.ethz.sis.rocrateserver.openapi.v1.service.delegates;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.AsyncJobRegistry;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ImportJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import edu.kit.datamanager.ro_crate.RoCrate;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ImportJob.getRoCrate;

@ApplicationScoped
public class ImportDelegate
{
    private static final Logger LOG = Logger.getLogger(ImportDelegate.class);

    public static class OpenBisImportResult
    {
        List<String> identifiers;

        Map<String, String> externalToOpenBisIdentifiers;

        ValidationResult validationResult;

        public OpenBisImportResult(List<String> identifiers,
                Map<String, String> externalToOpenBisIdentifiers,
                ValidationResult validationResult)
        {
            this.identifiers = identifiers;
            this.externalToOpenBisIdentifiers = externalToOpenBisIdentifiers;
            this.validationResult = validationResult;
        }

        public List<String> getIdentifiers()
        {
            return identifiers;
        }

        public void setIdentifiers(List<String> identifiers)
        {
            this.identifiers = identifiers;
        }

        public Map<String, String> getExternalToOpenBisIdentifiers()
        {
            return externalToOpenBisIdentifiers;
        }

        public void setExternalToOpenBisIdentifiers(
                Map<String, String> externalToOpenBisIdentifiers)
        {
            this.externalToOpenBisIdentifiers = externalToOpenBisIdentifiers;
        }

        public ValidationResult getValidationResult()
        {
            return validationResult;
        }

        public void setValidationResult(
                ValidationResult validationResult)
        {
            this.validationResult = validationResult;
        }
    }

    public AsyncJob import_(AsyncJobRegistry asyncJobRegistry,
            OpenBIS openBIS,
            ImportParams headers,
            InputStream body,
            boolean validateOnly)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

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
        InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
        InputStream is2 = new ByteArrayInputStream(baos.toByteArray());

        try
        {

            RoCrate crate = getRoCrate(headers, is1);
            SchemaFacade schemaFacade = SchemaFacade.of(crate);
            var types = schemaFacade.getTypes();
            var propertyTypes = schemaFacade.getPropertyTypes();
            if (types == null || propertyTypes == null)
            {
                throw new IllegalArgumentException(
                        "Types and/or property types missing from crates");
            }

        } catch (Exception e)
        {
            Log.error("Problem for user " + System.getProperty(
                    "user.name")); //platform independent
            LOG.error("Could not open RO-Crate", e);
            RoCrateExceptions.throwInstance(RoCrateExceptions.MALFORMED_INPUT);

        }

        String userName = openBIS.getSessionInformation().getUserName();
        ImportJob importJob = new ImportJob(userName, headers, is2, openBIS, validateOnly);
        String jobId = asyncJobRegistry.register(importJob);
        return new AsyncJob(jobId);


    }

}
