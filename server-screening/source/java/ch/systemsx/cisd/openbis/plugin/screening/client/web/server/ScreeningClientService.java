/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.plugin.screening.client.web.server;

import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

import ch.rinn.restrictions.Private;
import ch.systemsx.cisd.common.servlet.IRequestContextProvider;
import ch.systemsx.cisd.openbis.common.spring.IUncheckedMultipartFile;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.IResultSetConfig;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.ResultSet;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.TableExportCriteria;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.TypedTableResultSet;
import ch.systemsx.cisd.openbis.generic.client.web.client.exception.UserFailureException;
import ch.systemsx.cisd.openbis.generic.client.web.server.AbstractClientService;
import ch.systemsx.cisd.openbis.generic.client.web.server.UploadedFilesBean;
import ch.systemsx.cisd.openbis.generic.client.web.server.resultset.DataProviderAdapter;
import ch.systemsx.cisd.openbis.generic.client.web.server.resultset.ITableModelProvider;
import ch.systemsx.cisd.openbis.generic.shared.ICommonServer;
import ch.systemsx.cisd.openbis.generic.shared.IServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AsyncBatchRegistrationResult;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.BatchRegistrationResult;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.CodeAndLabel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleParentWithDerived;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModelRowWithObject;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Vocabulary;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ExperimentIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ExperimentIdentifierFactory;
import ch.systemsx.cisd.openbis.plugin.screening.BuildAndEnvironmentInfo;
import ch.systemsx.cisd.openbis.plugin.screening.client.web.client.IScreeningClientService;
import ch.systemsx.cisd.openbis.plugin.screening.shared.IScreeningServer;
import ch.systemsx.cisd.openbis.plugin.screening.shared.ResourceNames;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.AnalysisProcedures;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.DatasetReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorDataset;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorValues;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageDatasetEnrichedReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageResolution;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageSampleContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.LibraryRegistrationInfo;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.LogicalImageInfo;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.NewLibrary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateImages;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ScreeningConstants;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellLocation;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellMetadata;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellReplicaImage;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.AnalysisProcedureCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.ExperimentSearchCriteria;

/**
 * The {@link IScreeningClientService} implementation.
 * 
 * @author Tomasz Pylak
 */
@Component(value = ResourceNames.SCREENING_PLUGIN_SERVICE)
public final class ScreeningClientService extends AbstractClientService implements
        IScreeningClientService
{
    @Resource(name = ch.systemsx.cisd.openbis.generic.shared.ResourceNames.COMMON_SERVER)
    private ICommonServer commonServer;

    @Resource(name = ResourceNames.SCREENING_PLUGIN_SERVER)
    private IScreeningServer server;

    public ScreeningClientService()
    {
    }

    @Private
    ScreeningClientService(final IScreeningServer server,
            final IRequestContextProvider requestContextProvider)
    {
        super(requestContextProvider);
        this.server = server;
    }

    //
    // AbstractClientService
    //

    @Override
    protected final IServer getServer()
    {
        return server;
    }

    //
    // IScreeningClientService
    //

    @Override
    protected String getVersion()
    {
        return BuildAndEnvironmentInfo.INSTANCE.getFullVersion();
    }

    @Override
    public final SampleParentWithDerived getSampleGenerationInfo(final TechId sampleId)
            throws UserFailureException
    {
        return server.getSampleInfo(getSessionToken(), sampleId);
    }

    @Override
    public AbstractExternalData getDataSetInfo(TechId datasetTechId)
    {
        return server.getDataSetInfo(getSessionToken(), datasetTechId);
    }


    @Override
    public FeatureVectorDataset getFeatureVectorDataset(DatasetReference dataset,
            CodeAndLabel featureName) throws UserFailureException
    {
        return server.getFeatureVectorDataset(getSessionToken(), dataset, featureName);
    }

    @Override
    public FeatureVectorValues getWellFeatureVectorValues(String datasetCode, String datastoreCode,
            WellLocation location)
    {
        return server.getWellFeatureVectorValues(getSessionToken(), datasetCode, datastoreCode,
                location);
    }

    @Override
    public String prepareExportPlateWells(
            TableExportCriteria<TableModelRowWithObject<WellContent>> criteria)
    {
        return prepareExportEntities(criteria);
    }

    @Override
    public String prepareExportPlateMetadata(
            TableExportCriteria<TableModelRowWithObject<WellMetadata>> criteria)
    {
        return prepareExportEntities(criteria);
    }


    public List<BatchRegistrationResult> registerLibrary(LibraryRegistrationInfo details, String sessionKey, boolean async, String userEmail)
                throws UserFailureException {
        throw new IllegalStateException("GWT removed");
    }
//    Remove GWT
//    @Override
//    public List<BatchRegistrationResult> registerLibrary(LibraryRegistrationInfo details, String sessionKey, boolean async, String userEmail)
//            throws UserFailureException
//    {
//        final String sessionToken = getSessionToken();
//        HttpSession session = getHttpSession();
//        UploadedFilesBean uploadedFiles = null;
//        String experiment = details.getExperiment();
//
//        try
//        {
//            ExperimentIdentifier experimentIdentifier = new ExperimentIdentifierFactory(experiment).createIdentifier();
//            String space = experimentIdentifier.getSpaceCode();
//            boolean projectSamplesEnabled = server.isProjectSamplesEnabled(sessionToken);
//            String sampleProject = projectSamplesEnabled ? experimentIdentifier.getProjectCode() : null;
//            uploadedFiles = getUploadedFiles(sessionKey, session);
//
//            List<NewLibrary> newLibraries = new LinkedList<NewLibrary>();
//            List<BatchRegistrationResult> results = new LinkedList<BatchRegistrationResult>();
//
//            for (IUncheckedMultipartFile file : uploadedFiles.iterable())
//            {
//                LibraryExtractor extractor =
//                        new LibraryExtractor(file.getInputStream(), details.getSeparator(),
//                                experiment, space, sampleProject, details.getPlateGeometry(), details.getScope());
//                extractor.extract();
//
//                NewLibrary newLibrary = new NewLibrary();
//                newLibrary.setNewGenesOrNull(extractor.getNewGenes());
//                newLibrary.setNewOligosOrNull(extractor.getNewOligos());
//                newLibrary.setNewSamplesWithType(extractor.getNewSamplesWithType());
//                newLibraries.add(newLibrary);
//
//                BatchRegistrationResult result = new BatchRegistrationResult();
//                result.setFileName(file.getOriginalFilename());
//                result.setMessage(String.format("%d gene(s), %d oligo(s) %d sample(s) found and registered.", newLibrary.getNewGenesCount(),
//                        newLibrary.getNewOligosCount(), newLibrary.getNewSamplesWithTypeCount()));
//                results.add(result);
//            }
//
//            if (async)
//            {
//                server.registerLibrariesAsync(sessionToken, newLibraries, userEmail);
//                String fileName = results.get(0).getFileName();
//                return AsyncBatchRegistrationResult.singletonList(fileName);
//            } else
//            {
//                server.registerLibraries(sessionToken, newLibraries);
//                return results;
//            }
//        } finally
//        {
//            cleanUploadedFiles(sessionKey, session, uploadedFiles);
//        }
//    }

    @Override
    public Vocabulary getPlateGeometryVocabulary() throws UserFailureException
    {
        final String sessionToken = getSessionToken();
        return server.getVocabulary(sessionToken, ScreeningConstants.PLATE_GEOMETRY);
    }

    @Override
    public LogicalImageInfo getImageDatasetInfo(String datasetCode, String datastoreCode,
            WellLocation wellLocationOrNull)
    {
        final String sessionToken = getSessionToken();
        return server.getImageDatasetInfo(sessionToken, datasetCode, datastoreCode,
                wellLocationOrNull);
    }

    @Override
    public ImageDatasetEnrichedReference getImageDatasetReference(String datasetCode,
            String datastoreCode)
    {
        final String sessionToken = getSessionToken();
        return server.getImageDatasetReference(sessionToken, datasetCode, datastoreCode);
    }

    @Override
    public List<ImageResolution> getImageDatasetResolutions(String datasetCode, String datastoreCode)
    {
        final String sessionToken = getSessionToken();
        return server.getImageDatasetResolutions(sessionToken, datasetCode, datastoreCode);
    }

    @Override
    public ImageSampleContent getImageDatasetInfosForSample(TechId sampleId,
            WellLocation wellLocationOrNull)
    {
        final String sessionToken = getSessionToken();
        return server.getImageDatasetInfosForSample(sessionToken, sampleId, wellLocationOrNull);
    }



    @Override
    public AnalysisProcedures listNumericalDatasetsAnalysisProcedures(
            ExperimentSearchCriteria experimentSearchCriteria)
    {
        return server.listNumericalDatasetsAnalysisProcedures(getSessionToken(),
                experimentSearchCriteria);
    }
}
