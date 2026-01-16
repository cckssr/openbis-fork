/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.dss.systemtest.api.v3;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.MultiPartRequestContent;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.Material;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.delete.MaterialDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.id.IMaterialId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.id.MaterialPermId;
import ch.systemsx.cisd.common.utilities.TestResources;

/**
 * @author pkupczyk
 */
public class GeneralImportTest extends ObjectsImportTest
{

    @Test(dataProvider = FALSE_TRUE_PROVIDER)
    public void testImport(boolean async) throws Exception
    {
        String sessionToken = as.login(TEST_USER, PASSWORD);

        MaterialPermId materialPermId1 = new MaterialPermId("TEST-IMPORT-1", "VIRUS");
        MaterialPermId materialPermId2 = new MaterialPermId("TEST-IMPORT-2", "VIRUS");

        deleteMaterials(sessionToken, materialPermId1, materialPermId2);

        try
        {
            Material material1 = getObject(sessionToken, materialPermId1);
            assertNull(material1);

            Material material2 = getObject(sessionToken, materialPermId2);
            assertNull(material2);

            TestResources resources = new TestResources(getClass());
            File materialsFile = resources.getResourceFile("materials_excel_97_2003.xls");

            MultiPartRequestContent multiPart = new MultiPartRequestContent();

            byte[] bytes = FileUtils.readFileToByteArray(materialsFile);
            multiPart.addPart(new MultiPart.ContentSourcePart(
                    TEST_UPLOAD_KEY,                                // field name
                    materialsFile.getName(),                        // filename
                    HttpFields.EMPTY,                               // headers (none)
                    new BytesRequestContent(bytes)

            ));

            multiPart.close();

            uploadFiles(sessionToken, TEST_UPLOAD_KEY, multiPart);
            assertUploadedFiles(sessionToken, FileUtils.readFileToString(materialsFile));

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put(PARAM_UPLOAD_KEY, TEST_UPLOAD_KEY);
            parameters.put(PARAM_UPDATE_EXISTING, false);
            parameters.put(PARAM_ASYNC, async);

            if (async)
            {
                parameters.put(PARAM_USER_EMAIL, TEST_EMAIL);
            }

            long timestamp = getTimestampAndWaitASecond();
            String message = executeImport(sessionToken, "generalImport", parameters);

            material1 = getObject(sessionToken, materialPermId1, timestamp, DEFAULT_TIMEOUT);
            assertEquals("imported description 1", material1.getProperty("DESCRIPTION"));

            material2 = getObject(sessionToken, materialPermId2, timestamp, DEFAULT_TIMEOUT);
            assertEquals("default imported description", material2.getProperty("DESCRIPTION"));

            if (async)
            {
                assertEquals("When the import is complete the confirmation or failure report will be sent by email.", message);
                assertEmail(timestamp, TEST_EMAIL, "General Batch Import successfully performed");
            } else
            {
                assertEquals("Registration/update of 2 material(s) is complete.\nRegistration of 0 sample(s) is complete.", message);
                assertNoEmails(timestamp);
            }

            assertUploadedFiles(sessionToken);

        } finally
        {
            deleteMaterials(sessionToken, materialPermId1, materialPermId2);
        }
    }

    private void deleteMaterials(String sessionToken, IMaterialId... materialIds)
    {
        MaterialDeletionOptions options = new MaterialDeletionOptions();
        options.setReason("cleanup");
        as.deleteMaterials(sessionToken, Arrays.asList(materialIds), options);
    }

}
