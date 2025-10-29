package ch.ethz.sis.openbis.afsserver.server.common;

import java.util.List;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
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

public interface IOpenBISFacade
{
    SearchResult<Event> searchEvents(EventSearchCriteria criteria, EventFetchOptions fetchOptions);

    SearchResult<Experiment> searchExperiments(ExperimentSearchCriteria criteria, ExperimentFetchOptions fetchOptions);

    SearchResult<Sample> searchSamples(SampleSearchCriteria criteria, SampleFetchOptions fetchOptions);

    SearchResult<DataSet> searchDataSets(DataSetSearchCriteria criteria, DataSetFetchOptions fetchOptions);

    SearchResult<Tag> searchTags(TagSearchCriteria criteria, TagFetchOptions fetchOptions);

    SearchResult<DataStore> searchDataStores(DataStoreSearchCriteria criteria, DataStoreFetchOptions fetchOptions);

    void updateDataSets(List<DataSetUpdate> updates);

    String getSessionToken();
}
