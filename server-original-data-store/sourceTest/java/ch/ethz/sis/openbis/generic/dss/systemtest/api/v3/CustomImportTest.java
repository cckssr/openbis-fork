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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jetty.client.MultiPartRequestContent;
import org.eclipse.jetty.client.StringRequestContent;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.delete.DataSetDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;

/**
 * @author pkupczyk
 */
public class CustomImportTest extends ObjectsImportTest
{

    @Test(dataProvider = FALSE_TRUE_PROVIDER)
    public void testImport(boolean async) throws Exception
    {
        String sessionToken = as.login(TEST_USER, PASSWORD);

        DataSetPermId dataSetPermId = new DataSetPermId("TEST-IMPORT-" + UUID.randomUUID().toString());

        try
        {
//            MultiPartContentProvider multiPart = new MultiPartContentProvider();
//            ContentProvider contentProvider = new StringContentProvider("test-file-content");
//            multiPart.addFilePart(TEST_UPLOAD_KEY, dataSetPermId.getPermId(), contentProvider, null);
//            multiPart.close();

            MultiPartRequestContent multiPart = new MultiPartRequestContent();
            multiPart.addPart(new MultiPart.ContentSourcePart(
                    TEST_UPLOAD_KEY,                     // field name
                    dataSetPermId.getPermId(),           // filename
                    HttpFields.EMPTY,                    // no extra headers
                    new StringRequestContent("test-file-content") // content (defaults to text/plain)
            ));
            multiPart.close();

            uploadFiles(sessionToken, TEST_UPLOAD_KEY, multiPart);
            assertUploadedFiles(sessionToken, "test-file-content");

            DataSet dataSet = getObject(sessionToken, dataSetPermId);
            assertNull(dataSet);

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put(PARAM_UPLOAD_KEY, TEST_UPLOAD_KEY);
            parameters.put(PARAM_CUSTOM_IMPORT_CODE, "test-custom-import");
            parameters.put(PARAM_ASYNC, async);

            if (async)
            {
                parameters.put(PARAM_USER_EMAIL, TEST_EMAIL);
            }

            long timestamp = getTimestampAndWaitASecond();
            String message = executeImport(sessionToken, "customImport", parameters);

            dataSet = getObject(sessionToken, dataSetPermId, timestamp, DEFAULT_TIMEOUT);
            assertEquals("test comment " + dataSetPermId.getPermId(), dataSet.getProperty("COMMENT"));

            if (async)
            {
                assertEquals("When the import is complete the confirmation or failure report will be sent by email.", message);
                getTimestampAndWaitASecond();
                assertEmail(timestamp, TEST_EMAIL, "Custom import successfully performed");
            } else
            {
                assertEquals("Import successfully completed.", message);
                assertNoEmails(timestamp);
            }

            assertUploadedFiles(sessionToken);

        } finally
        {
            DataSetDeletionOptions options = new DataSetDeletionOptions();
            options.setReason("cleanup");
            as.deleteDataSets(sessionToken, Arrays.asList(dataSetPermId), options);
        }
    }

}
