/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.plugin.screening.server.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.systemsx.cisd.common.collection.IKeyExtractor;
import ch.systemsx.cisd.common.collection.TableMap;
import ch.systemsx.cisd.common.collection.TableMap.UniqueKeyViolationStrategy;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.IDataSetTable;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.CodeAndLabel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModelColumnHeader;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModelRow;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import ch.systemsx.cisd.openbis.plugin.screening.server.IScreeningBusinessObjectFactory;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.IScreeningQuery;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ExperimentFeatureVectorSummary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ExperimentReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.AnalysisProcedureCriteria;

/**
 * {@See #loadExperimentFeatureVectors}.
 * 
 * @author Tomasz Pylak
 */
public class ExperimentFeatureVectorSummaryLoader extends AbstractContentLoader
{

    private final WellDataLoader wellDataLoader;

    public ExperimentFeatureVectorSummaryLoader(Session session,
            IScreeningBusinessObjectFactory businessObjectFactory, IDAOFactory daoFactory,
            IScreeningQuery screeningQuery)
    {
        super(session, businessObjectFactory, daoFactory, screeningQuery);
        this.wellDataLoader =
                new WellDataLoader(session, businessObjectFactory, daoFactory, screeningQuery);
    }

    private UserFailureException decorateException(Exception ex, AbstractExternalData dataSet,
            String message)
    {
        return new UserFailureException("Analysis summary for data set " + dataSet.getCode()
                + " couldn't retrieved from Data Store Server. " + message, ex);
    }

    private List<AbstractExternalData> getMatchingDataSets(TechId experimentId,
            AnalysisProcedureCriteria analysisProcedureCriteria, AnalysisSettings analysisSettings)
    {
        List<AbstractExternalData> dataSets =
                businessObjectFactory.createDatasetLister(session).listByExperimentTechId(
                        experimentId, true);
        List<AbstractExternalData> matchingDataSets = new ArrayList<AbstractExternalData>();
        for (AbstractExternalData dataSet : dataSets)
        {
            if (ScreeningUtils.isMatchingAnalysisProcedure(dataSet, analysisProcedureCriteria)
                    && analysisSettings.tryToGetReportingPluginKey(dataSet) != null)
            {
                matchingDataSets.add(dataSet);
            }
        }
        return matchingDataSets;
    }

}
