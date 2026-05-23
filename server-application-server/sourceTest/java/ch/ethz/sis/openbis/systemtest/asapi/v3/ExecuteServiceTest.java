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
package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id.CustomASServiceCode;

import java.util.*;

/**
 * @author Franz-Josef Elmer
 */
public class ExecuteServiceTest extends AbstractTest
{

    @Test
    public void testSearchServices()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        CustomASServiceExecutionOptions options = new CustomASServiceExecutionOptions();
        options.withParameter("name", Math.PI);

        Object result = v3api.executeCustomASService(sessionToken, new CustomASServiceCode("simple-service"), options);

        assertEquals(result, "hello 3.14159265359. Spaces: [Space CISD]");
    }

    @Test
    public void testLogging()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        CustomASServiceExecutionOptions o = new CustomASServiceExecutionOptions();
        // values of these parameters must not be logged as they can contain sensitive data
        o.withParameter("a", "1");
        o.withParameter("b", "2");

        v3api.executeCustomASService(sessionToken, new CustomASServiceCode("simple-service"), o);

        assertAccessLog(
                "execute-custom-as-service  SERVICE_ID('simple-service') EXECUTION_OPTIONS('CustomASServiceExecutionOptions: parameterKeys=[a, b]')");
    }

    @Test
    public void testExecuteNonExistingService()
    {
        assertUserFailureException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                String sessionToken = v3api.login(TEST_USER, PASSWORD);

                CustomASServiceCode code = new CustomASServiceCode("non_existing_service_code");
                CustomASServiceExecutionOptions options = new CustomASServiceExecutionOptions();
                options.withParameter("non_existing_service_code_param_key", "non_existing_service_code_param_value");
                v3api.executeCustomASService(sessionToken, code, options);

            }
        }, "Object with CustomASServiceCode = [non_existing_service_code] has not been found.");

    }

    @Test
    public void testExecuteEntityCollectorExtendedService()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);


        final SampleCreation creation = new SampleCreation();
        creation.setCode("SAMPLE_FOR_ENTITY_COLLECTOR");
        creation.setTypeId(new EntityTypePermId("CELL_PLATE"));
        creation.setSpaceId(new SpacePermId("CISD"));
        creation.setExperimentId(new ExperimentIdentifier("/CISD/NEMO/EXP1"));

        List<SamplePermId> permIds = v3api.createSamples(sessionToken, Collections.singletonList(creation));

        assertSamplesExists(permIds.get(0).getPermId());

        CustomASServiceCode code = new CustomASServiceCode("entity-collector-extended");
        CustomASServiceExecutionOptions options = new CustomASServiceExecutionOptions();

        Map<String, Object> params = new HashMap<>();
        params.put("kind", "SAMPLE");
        params.put("permId", permIds.get(0).getPermId());
        params.put("withLevelsAbove", false);
        params.put("withLevelsBelow", false);
        params.put("withObjectsAndDataSetsParents", false);
        params.put("withObjectsAndDataSetsChildren", false);
        params.put("withObjectsAndDataSetsOtherSpaces", false);
        List<Map<String, Object>> nodeExportMaps = new ArrayList<>();
        nodeExportMaps.add(params);

        options.withParameter("nodeExportList", nodeExportMaps);
        Object serviceResult = v3api.executeCustomASService(sessionToken, code, options);

        assertTrue(serviceResult instanceof HashSet);
        Set<ExportablePermId> result = (Set<ExportablePermId>) serviceResult;
        AssertJUnit.assertEquals(1, result.size());
        ExportablePermId permId = result.iterator().next();
        AssertJUnit.assertEquals(ExportableKind.SAMPLE, permId.getExportableKind());
        AssertJUnit.assertEquals(permIds.get(0).getPermId(), permId.getPermId());

    }

}