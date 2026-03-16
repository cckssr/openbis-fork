/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.plugin.screening.shared;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.IServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.CodeAndLabel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleParentWithDerived;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Vocabulary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.AnalysisProcedures;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.DatasetReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ExperimentFeatureVectorSummary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorDataset;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorValues;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageDatasetEnrichedReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageResolution;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageSampleContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.LogicalImageInfo;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.NewLibrary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateImages;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellLocation;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellReplicaImage;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.AnalysisProcedureCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.ExperimentSearchCriteria;

/**
 * The <i>screening</i> server. Used internally.
 * 
 * @author Tomasz Pylak
 */
public interface IScreeningServer extends IServer
{

    /**
     * Loads feature vector of specified dataset with one feature specified by name.
     */
    @Transactional(readOnly = true)
    public FeatureVectorDataset getFeatureVectorDataset(String sessionToken,
            DatasetReference dataset, CodeAndLabel featureName);

    /**
     * Loads all feature vector values for specified well.
     */
    // TODO can return null
    @Transactional(readOnly = true)
    public FeatureVectorValues getWellFeatureVectorValues(String sessionToken,
            String datasetCode, String datastoreCode, WellLocation wellLocation);

    @Transactional
    public LogicalImageInfo getImageDatasetInfo(String sessionToken,
            String datasetCode, String datastoreCode, WellLocation wellLocationOrNull);

    @Transactional
    public ImageDatasetEnrichedReference getImageDatasetReference(String sessionToken,
            String datasetCode, String datastoreCode);

    @Transactional
    public List<ImageResolution> getImageDatasetResolutions(String sessionToken,
            String datasetCode, String datastoreCode);

    @Transactional
    public ImageSampleContent getImageDatasetInfosForSample(String sessionToken,
            TechId sampleId, WellLocation wellLocationOrNull);

    /**
     * For given {@link TechId} returns the {@link Sample} and its derived (child) samples.
     * 
     * @return never <code>null</code>.
     * @throws UserFailureException if given <var>sessionToken</var> is invalid or whether sample uniquely identified by given <var>sampleId</var>
     *             does not exist.
     */
    @Transactional(readOnly = true)
    public SampleParentWithDerived getSampleInfo(final String sessionToken,
            final TechId sampleId) throws UserFailureException;

    /**
     * For given {@link TechId} returns the corresponding {@link AbstractExternalData}.
     */
    @Transactional(readOnly = true)
    public AbstractExternalData getDataSetInfo(String sessionToken,
            TechId datasetId);

    /**
     * Returns vocabulary with given code.
     */
    @Transactional
    public Vocabulary getVocabulary(String sessionToken, String code) throws UserFailureException;

    /**
     * Registers the contents of uploaded libraries.
     */
    @Transactional
    public void registerLibraries(String sessionToken, List<NewLibrary> newLibraries);

    /**
     * Asynchronously registers the contents of uploaded libraries.
     */
    @Transactional
    public void registerLibrariesAsync(String sessionToken, List<NewLibrary> newLibraries, String userEmail);

    /**
     * Return a list of all different analysis procedures applied to the well analysis data sets of an experiment.
     * <p>
     * Note that analysis procedures of segmentation image datasets are not returned by this method!
     * </p>
     * <p>
     * The result contains unique values. It can contain NULL (which can be used for data sets having no ANALYSIS_PROCEDURE value specified).
     * </p>
     */
    @Transactional(readOnly = true)
    public AnalysisProcedures listNumericalDatasetsAnalysisProcedures(String sessionToken,
            ExperimentSearchCriteria experimentSearchCriteria);

}
