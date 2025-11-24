package ch.ethz.sis.rocrateserver.openapi.v1.service.delegates;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.AsyncJobRegistry;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ImportJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
        String userName = openBIS.getSessionInformation().getUserName();
        ImportJob importJob = new ImportJob(userName, headers, body, openBIS, validateOnly);
        String jobId = asyncJobRegistry.register(importJob);
        return new AsyncJob(jobId);


    }

}
