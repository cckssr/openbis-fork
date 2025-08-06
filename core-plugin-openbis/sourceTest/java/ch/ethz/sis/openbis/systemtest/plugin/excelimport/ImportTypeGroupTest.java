/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.systemtest.plugin.excelimport;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(locations = "classpath:applicationContext.xml")
@Transactional(transactionManager = "transaction-manager")
@Rollback
public class ImportTypeGroupTest extends AbstractImportTest
{
    @Autowired
    private IApplicationServerInternalApi v3api;

    private static final String TYPE_GROUPS_XLS = "type_groups/normal_type_group.xls";


    private static String FILES_DIR;

    @BeforeClass
    public void setupClass() throws IOException
    {
        String f = ImportTypeGroupTest.class.getName().replace(".", "/");
        FILES_DIR = f.substring(0, f.length() - ImportTypeGroupTest.class.getSimpleName().length()) + "/test_files/";
    }

    @Test
    @DirtiesContext
    public void testNormalTypeGroupsAreCreated() throws Exception
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, TYPE_GROUPS_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        TypeGroup group = TestUtils.getTypeGroup(v3api, sessionToken, "TEST_IMPORT_GROUP");
        assertEquals(group.getId().getPermId(), "TEST_IMPORT_GROUP");
        assertEquals(group.getCode(), "TEST_IMPORT_GROUP");
        assertEquals(group.getRegistrator().getUserId(), "system");
        assertFalse(group.isManagedInternally());


        group = TestUtils.getTypeGroup(v3api, sessionToken, "TEST_IMPORT_GROUP_INTERNAL");
        assertEquals(group.getId().getPermId(), "TEST_IMPORT_GROUP_INTERNAL");
        assertEquals(group.getCode(), "TEST_IMPORT_GROUP_INTERNAL");
        assertEquals(group.getRegistrator().getUserId(), "system");
        assertTrue(group.isManagedInternally());
    }
}
