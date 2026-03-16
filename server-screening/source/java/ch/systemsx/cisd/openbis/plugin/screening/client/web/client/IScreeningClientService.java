/*
 * Copyright ETH 2007 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.plugin.screening.client.web.client;

import java.util.List;

import ch.systemsx.cisd.openbis.generic.client.web.client.IClientService;
import ch.systemsx.cisd.openbis.generic.client.web.client.ICommonClientService;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.IResultSetConfig;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.TableExportCriteria;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.TypedTableResultSet;
import ch.systemsx.cisd.openbis.generic.client.web.client.exception.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.BatchRegistrationResult;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.CodeAndLabel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleParentWithDerived;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModelRowWithObject;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Vocabulary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.AnalysisProcedures;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.DatasetReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorDataset;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorValues;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageDatasetEnrichedReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageResolution;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageSampleContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.LibraryRegistrationInfo;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.LogicalImageInfo;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateImages;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellLocation;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellMetadata;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellReplicaImage;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.AnalysisProcedureCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.ExperimentSearchCriteria;

/**
 * Service interface for the <i>screening</i> <i>GWT</i> client.
 * <p>
 * Each method should declare throwing {@link UserFailureException}. The authorisation framework can throw it when the user has insufficient
 * privileges. If it is not marked, the GWT client will report unexpected exception.
 * </p>
 * 
 * @author Tomasz Pylak
 */
public interface IScreeningClientService extends IClientService
{

    /**
     * For given {@link TechId} returns corresponding {@link SampleParentWithDerived}.
     */
    public SampleParentWithDerived getSampleGenerationInfo(final TechId sampleId)
            throws UserFailureException;

    /**
     * For given {@link TechId} returns corresponding {@link AbstractExternalData}.
     */
    public AbstractExternalData getDataSetInfo(TechId datasetTechId) throws UserFailureException;

    /**
     * Fetches feature vector of specified dataset with one feature specified by name.
     */
    public FeatureVectorDataset getFeatureVectorDataset(DatasetReference dataset,
            CodeAndLabel featureName);

    /**
     * Fetches feature vector of specified dataset with one feature specified by name.
     */
    public FeatureVectorValues getWellFeatureVectorValues(String datasetCode, String datastoreCode,
            WellLocation location);

    public String prepareExportPlateWells(
            TableExportCriteria<TableModelRowWithObject<WellContent>> criteria)
            throws UserFailureException;

    /**
     * Like {@link ICommonClientService#prepareExportSamples(TableExportCriteria)}, but for TypedTableResultSet.
     */
    public String prepareExportPlateMetadata(
            TableExportCriteria<TableModelRowWithObject<WellMetadata>> exportCriteria)
            throws UserFailureException;

    /**
     * Returns information about logical image in the given dataset. In HCS case the well location should be specified.
     */
    public LogicalImageInfo getImageDatasetInfo(String datasetCode, String datastoreCode,
            WellLocation wellLocationOrNull) throws UserFailureException;

    /**
     * Returns information about image dataset for a given image dataset. Used to refresh information about the dataset.
     */
    public ImageDatasetEnrichedReference getImageDatasetReference(String datasetCode,
            String datastoreCode);

    /**
     * Returns information about available image resolutions for a given image dataset.
     */
    public List<ImageResolution> getImageDatasetResolutions(String datasetCode, String datastoreCode);

    /**
     * Loads information about datasets connected to specified sample (microscopy) or a container sample (HCS). In particular loads the logical images
     * in datasets belonging to the specified sample (restricted to one well in HCS case).
     */
    public ImageSampleContent getImageDatasetInfosForSample(TechId sampleId,
            WellLocation wellLocationOrNull);

    /**
     * Registers a new library.
     */
    public List<BatchRegistrationResult> registerLibrary(LibraryRegistrationInfo newLibraryInfo,
            String sessionKey, boolean async, String userEmail) throws UserFailureException;

    /**
     * Returns plate geometry vocabulary.
     */
    public Vocabulary getPlateGeometryVocabulary() throws UserFailureException;


    /**
     * Return all analysis procedures for an experiment criteria.
     */
    public AnalysisProcedures listNumericalDatasetsAnalysisProcedures(
            ExperimentSearchCriteria experimentSearchCriteria);

}
