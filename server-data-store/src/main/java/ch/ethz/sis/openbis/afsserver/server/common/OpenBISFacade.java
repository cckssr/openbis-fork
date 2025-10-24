package ch.ethz.sis.openbis.afsserver.server.common;

import java.util.List;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.Event;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.fetchoptions.EventFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.search.EventSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.Tag;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.fetchoptions.TagFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.search.TagSearchCriteria;
import ch.systemsx.cisd.common.exceptions.InvalidSessionException;

public class OpenBISFacade implements IOpenBISFacade
{

    private final String openBISUrl;

    private final String openBISUser;

    private final String openBISPassword;

    private final Integer openBISTimeout;

    private volatile String sessionToken;

    public OpenBISFacade(String openBISUrl, String openBISUser, String openBISPassword, Integer openBISTimeout)
    {
        this.openBISUrl = openBISUrl;
        this.openBISUser = openBISUser;
        this.openBISPassword = openBISPassword;
        this.openBISTimeout = openBISTimeout;
    }

    @Override public SearchResult<Event> searchEvents(EventSearchCriteria criteria, EventFetchOptions fetchOptions)
    {
        return executeOperation(openBIS -> openBIS.searchEvents(criteria, fetchOptions));
    }

    @Override public SearchResult<Experiment> searchExperiments(ExperimentSearchCriteria criteria, ExperimentFetchOptions fetchOptions)
    {
        return executeOperation(openBIS -> openBIS.searchExperiments(criteria, fetchOptions));
    }

    @Override public SearchResult<Sample> searchSamples(SampleSearchCriteria criteria, SampleFetchOptions fetchOptions)
    {
        return executeOperation(openBIS -> openBIS.searchSamples(criteria, fetchOptions));
    }

    @Override public SearchResult<DataSet> searchDataSets(DataSetSearchCriteria criteria, DataSetFetchOptions fetchOptions)
    {
        return executeOperation(openBIS -> openBIS.searchDataSets(criteria, fetchOptions));
    }

    @Override public SearchResult<Tag> searchTags(TagSearchCriteria criteria, TagFetchOptions fetchOptions)
    {
        return executeOperation(openBIS -> openBIS.searchTags(criteria, fetchOptions));
    }

    @Override public void updateDataSets(final List<DataSetUpdate> updates)
    {
        executeOperation(openBIS ->
        {
            openBIS.updateDataSets(updates);
            return null;
        });
    }

    private <T> T executeOperation(Operation<T> operation)
    {
        OpenBIS openBIS = new OpenBIS(openBISUrl, openBISTimeout);
        setSessionToken(openBIS);

        try
        {
            return operation.execute(openBIS);
        } catch (InvalidSessionException e)
        {
            resetSessionToken(openBIS);
            return operation.execute(openBIS);
        }
    }

    private interface Operation<T>
    {
        T execute(OpenBIS openBIS);
    }

    private void setSessionToken(OpenBIS openBIS)
    {
        synchronized (this)
        {
            if (sessionToken != null)
            {
                openBIS.setSessionToken(sessionToken);
            } else
            {
                sessionToken = openBIS.login(openBISUser, openBISPassword);

                if (sessionToken != null)
                {
                    openBIS.setSessionToken(sessionToken);
                } else
                {
                    throw new RuntimeException(
                            "Could not login to the AS server. Please check openBIS user and openBIS password in the AFS server configuration.");
                }
            }
        }
    }

    private void resetSessionToken(OpenBIS openBIS)
    {
        synchronized (this)
        {
            sessionToken = null;
            setSessionToken(openBIS);
        }
    }

    @Override public String getSessionToken()
    {
        OpenBIS openBIS = new OpenBIS(openBISUrl, openBISTimeout);
        setSessionToken(openBIS);

        if (!openBIS.isSessionActive())
        {
            resetSessionToken(openBIS);
        }

        return openBIS.getSessionToken();
    }

}