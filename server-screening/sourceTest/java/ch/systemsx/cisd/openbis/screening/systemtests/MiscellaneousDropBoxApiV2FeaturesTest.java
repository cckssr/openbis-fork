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
package ch.systemsx.cisd.openbis.screening.systemtests;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ListSampleCriteria;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ExperimentIdentifierFactory;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.DatasetImagesReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageDatasetEnrichedReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ImageResolution;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateContent;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.PlateMetadata;

/**
 * @author Franz-Josef Elmer
 */
@Test(groups =
        { "slow", "systemtest" })
public class MiscellaneousDropBoxApiV2FeaturesTest extends AbstractScreeningSystemTestCase
{
    @BeforeTest
    public void dropAnExampleDataSet() throws IOException, Exception
    {
        File exampleDataSet = createTestDataContents();
        moveFileToIncoming(exampleDataSet);
        waitUntilDataSetImported();
    }

    @AfterMethod
    public void tearDown()
    {
        File[] files = getIncomingDirectory().listFiles();
        for (File file : files)
        {
            FileUtilities.deleteRecursively(file);
        }
    }

    @Test
    public void testSettingPlateGeometryByFigureGeometry()
    {
        Experiment experiment =
                commonServer.getExperimentInfo(sessionToken,
                        ExperimentIdentifierFactory.parse("/TEST/TEST-PROJECT/TEST-EXP-HCS"));
        List<Sample> samples =
                commonServer.listSamples(sessionToken,
                        ListSampleCriteria.createForExperiment(new TechId(experiment.getId())));

        Sample sample = samples.get(0);
        assertEquals("/TEST/TEST-PROJECT/PLATE137", sample.getIdentifier());
        assertEquals("[PLATE_GEOMETRY: 24_WELLS_4X6]", sample.getProperties().toString());
        assertEquals(1, samples.size());
    }


    private File createTestDataContents() throws IOException
    {
        File dest = new File(workingDirectory, "TEST-PLATE1");
        dest.mkdirs();
        File src = new File(getTestDataFolder(), "PLATE1");

        // Copy the test data set to the location for processing
        FileUtils.copyDirectory(src, dest);
        return dest;
    }

    private String getTestDataFolder()
    {
        return "../server-screening/resource/test-data/" + getClass().getSimpleName() + "/";
    }

    @Override
    protected int dataSetImportWaitDurationInSeconds()
    {
        return 60;
    }
}
