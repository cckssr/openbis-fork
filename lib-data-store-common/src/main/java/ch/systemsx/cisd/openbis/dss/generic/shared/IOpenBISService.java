package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DeletedDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocationNode;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ExperimentIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.SampleIdentifier;

public interface IOpenBISService
{
    String getSessionToken();

    AbstractExternalData tryGetDataSet(String dataSetCode);

    IDatasetLocationNode tryGetDataSetLocation(String dataSetCode);

    Experiment tryGetExperiment(ExperimentIdentifier experimentIdentifier) throws UserFailureException;

    Sample tryGetSampleWithExperiment(SampleIdentifier sampleIdentifier) throws UserFailureException;

    List<AbstractExternalData> listDataSetsByCode(List<String> dataSetCodes);

    List<SimpleDataSetInformationDTO> listPhysicalDataSets() throws UserFailureException;

    List<DeletedDataSet> listDeletedDataSets(Long lastSeenDeletionEventIdOrNull, Date maxDeletionDateOrNull);

    void updateDataSetStatuses(List<String> dataSetCodes, DataSetArchivingStatus newStatus, boolean presentInArchive) throws UserFailureException;

    void updateShareIdAndSize(String dataSetCode, String shareId, long size);

    void archiveDataSets(List<String> dataSetCodes, boolean removeFromDataStore, Map<String, String> options) throws UserFailureException;

    void notifyDatasetAccess(String dataSetCode);
}
