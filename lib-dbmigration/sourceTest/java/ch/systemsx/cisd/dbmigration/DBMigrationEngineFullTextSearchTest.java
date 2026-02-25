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
package ch.systemsx.cisd.dbmigration;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.rinn.restrictions.Friend;
import ch.systemsx.cisd.common.db.ISqlScriptExecutor;
import ch.systemsx.cisd.dbmigration.java.IMigrationStepExecutor;

@Friend(toClasses = DBMigrationEngine.class)
/**
 * Tests only the full-text-search migration orchestration in {@link DBMigrationEngine}.
 *
 * It verifies that an existing FTS version file (e.g. 002) triggers a version upgrade run (e.g. 003),
 * that the main FTS script is requested via {@code applyFullTextSearchScripts(..., true)}, and that the
 * version file is updated. It does not execute real SQL or validate PostgreSQL behavior.
 */
public class DBMigrationEngineFullTextSearchTest
{
    private static final String TARGET_FULL_TEXT_SEARCH_VERSION = "003";

    private static final String PREVIOUS_FULL_TEXT_SEARCH_VERSION = "002";

    private Mockery context;

    private ISqlScriptProvider scriptProvider;

    private IDAOFactory daoFactory;

    private IDatabaseAdminDAO adminDAO;

    private IDatabaseVersionLogDAO logDAO;

    private ISqlScriptExecutor scriptExecutor;

    private IMigrationStepExecutor migrationStepExecutor;

    private IMigrationStepExecutor migrationStepExecutorAdmin;

    private File fullTextSearchVersionFile;

    private File fullTextSearchVersionFileBackup;

    @BeforeMethod
    public void setUp() throws Exception
    {
        context = new Mockery();
        scriptProvider = context.mock(ISqlScriptProvider.class);
        daoFactory = context.mock(IDAOFactory.class);
        adminDAO = context.mock(IDatabaseAdminDAO.class);
        logDAO = context.mock(IDatabaseVersionLogDAO.class);
        scriptExecutor = context.mock(ISqlScriptExecutor.class);
        migrationStepExecutor = context.mock(IMigrationStepExecutor.class, "migrationStepExecutor");
        migrationStepExecutorAdmin =
                context.mock(IMigrationStepExecutor.class, "migrationStepExecutorAdmin");

        fullTextSearchVersionFile =
                new File(DBMigrationEngine.FULL_TEXT_SEARCH_DOCUMENT_VERSION_FILE_PATH);
        fullTextSearchVersionFileBackup =
                new File(DBMigrationEngine.FULL_TEXT_SEARCH_DOCUMENT_VERSION_FILE_PATH + ".bak-test");
        if (fullTextSearchVersionFileBackup.exists())
        {
            fullTextSearchVersionFileBackup.delete();
        }
        if (fullTextSearchVersionFile.exists())
        {
            if (fullTextSearchVersionFile.renameTo(fullTextSearchVersionFileBackup) == false)
            {
                fail("Cannot back up " + fullTextSearchVersionFile.getPath());
            }
        }
    }

    @AfterMethod
    public void tearDown()
    {
        if (fullTextSearchVersionFile.exists())
        {
            fullTextSearchVersionFile.delete();
        }
        if (fullTextSearchVersionFileBackup.exists())
        {
            fullTextSearchVersionFile.getParentFile().mkdirs();
            if (fullTextSearchVersionFileBackup.renameTo(fullTextSearchVersionFile) == false)
            {
                fail("Cannot restore " + fullTextSearchVersionFile.getPath());
            }
        }
        context.assertIsSatisfied();
    }

    @Test
    public void testMainScriptExecutedForVersionUpgradeOnExistingDatabase() throws Exception
    {
        fullTextSearchVersionFile.getParentFile().mkdirs();
        Files.write(fullTextSearchVersionFile.toPath(),
                PREVIOUS_FULL_TEXT_SEARCH_VERSION.getBytes(StandardCharsets.UTF_8));

        context.checking(new Expectations()
        {
            {
                one(daoFactory).getDatabaseDAO();
                will(returnValue(adminDAO));
                one(daoFactory).getDatabaseVersionLogDAO();
                will(returnValue(logDAO));
                one(daoFactory).getSqlScriptExecutor();
                will(returnValue(scriptExecutor));
                one(daoFactory).getMigrationStepExecutor();
                will(returnValue(migrationStepExecutor));
                one(daoFactory).getMigrationStepExecutorAdmin();
                will(returnValue(migrationStepExecutorAdmin));

                one(adminDAO).applyFullTextSearchScripts(scriptProvider,
                        TARGET_FULL_TEXT_SEARCH_VERSION, true);
            }
        });

        final DBMigrationEngine migrationEngine =
                new DBMigrationEngine(daoFactory, scriptProvider, false);
        final Method method =
                DBMigrationEngine.class.getDeclaredMethod("migrateFullTextSearch", String.class);
        method.setAccessible(true);
        method.invoke(migrationEngine, TARGET_FULL_TEXT_SEARCH_VERSION);

        assertEquals(TARGET_FULL_TEXT_SEARCH_VERSION,
                new String(Files.readAllBytes(fullTextSearchVersionFile.toPath()),
                        StandardCharsets.UTF_8).trim());
    }
}
