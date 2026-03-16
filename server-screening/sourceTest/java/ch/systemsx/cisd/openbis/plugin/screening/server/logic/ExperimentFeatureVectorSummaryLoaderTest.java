/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.jmock.Expectations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.AbstractServerTestCase;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PhysicalDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModelColumnHeader;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.TableModelRow;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.builders.DataSetBuilder;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.builders.DataStoreBuilder;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExperimentPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExperimentTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ProjectPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import ch.systemsx.cisd.openbis.generic.shared.dto.SpacePE;
import ch.systemsx.cisd.openbis.plugin.screening.server.IScreeningBusinessObjectFactory;
import ch.systemsx.cisd.openbis.plugin.screening.server.ScreeningServer;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.IScreeningQuery;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ExperimentFeatureVectorSummary;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ExperimentReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ScreeningConstants;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.AnalysisProcedureCriteria;

/**
 * @author Franz-Josef Elmer
 */
public class ExperimentFeatureVectorSummaryLoaderTest extends AbstractServerTestCase
{
    private static final class ExperimentFeatureVectorSummaryLoaderWithNoCalculation extends ExperimentFeatureVectorSummaryLoader
    {
        public ExperimentFeatureVectorSummaryLoaderWithNoCalculation(Session session,
                IScreeningBusinessObjectFactory businessObjectFactory, IDAOFactory daoFactory,
                IScreeningQuery screeningQuery)
        {
            super(session, businessObjectFactory, daoFactory, screeningQuery);
        }


        ExperimentFeatureVectorSummary calculatedSummary(TechId experimentId,
                AnalysisProcedureCriteria analysisProcedureCriteria, ExperimentReference experiment)
        {
            return null;
        }
    }

    private static final String DATA_STORE_CODE = "DSS";

    private static final TechId EXPERIMENT_ID = new TechId(42);

    private IScreeningBusinessObjectFactory screeningBOFactory;

    private IScreeningQuery screeningQuery;

    @BeforeMethod
    public void beforeMethod()
    {
        screeningBOFactory = context.mock(IScreeningBusinessObjectFactory.class);
        screeningQuery = context.mock(IScreeningQuery.class);
    }


    private ExperimentFeatureVectorSummaryLoader createLoaderWithoutCalculation()
    {
        return new ExperimentFeatureVectorSummaryLoaderWithNoCalculation(session,
                screeningBOFactory, daoFactory, screeningQuery);
    }

    private void prepareLoadExperiment()
    {
        context.checking(new Expectations()
            {
                {
                    one(experimentDAO).tryGetById(EXPERIMENT_ID);
                    ExperimentPE experiment = new ExperimentPE();
                    experiment.setId(EXPERIMENT_ID.getId());
                    experiment.setPermId("123-" + EXPERIMENT_ID);
                    experiment.setExperimentType(new ExperimentTypePE());
                    ProjectPE project = new ProjectPE();
                    project.setSpace(new SpacePE());
                    experiment.setProject(project);
                    will(returnValue(experiment));
                }
            });
    }

    private void prepareListDataSetsByExperiment(final PhysicalDataSet... dataSets)
    {
        context.checking(new Expectations()
            {
                {
                    one(screeningBOFactory).createDatasetLister(session);
                    will(returnValue(datasetLister));

                    one(datasetLister).listByExperimentTechId(EXPERIMENT_ID, true);
                    will(returnValue(Arrays.asList(dataSets)));
                }
            });
    }

    private void prepareCreateReport(final TableModel report, final String reportingPluginKey,
            final PhysicalDataSet dataSet)
    {
        context.checking(new Expectations()
            {
                {
                    one(screeningBOFactory).createDataSetTable(session);
                    will(returnValue(dataSetTable));

                    one(dataSetTable).createReportFromDatasets(reportingPluginKey, DATA_STORE_CODE,
                            Arrays.asList(dataSet.getCode()));
                    will(returnValue(report));
                }
            });
    }

    private void prepareCreateReportFails(final Exception exception, final String reportingPluginKey,
            final PhysicalDataSet dataSet)
    {
        context.checking(new Expectations()
            {
                {
                    one(screeningBOFactory).createDataSetTable(session);
                    will(returnValue(dataSetTable));

                    one(dataSetTable).createReportFromDatasets(reportingPluginKey, DATA_STORE_CODE,
                            Arrays.asList(dataSet.getCode()));
                    will(throwException(exception));
                }
            });
    }

}
