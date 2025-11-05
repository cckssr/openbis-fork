/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.util;

import ch.systemsx.cisd.dbmigration.DBMigrationEngine;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.standard.utils.LogInitializer;

/**
 * @author Franz-Josef Elmer
 */
public class TestInitializer
{
    static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            TestInitializer.class);

    private static String dbKind = "test";

    private static boolean createDBFromScratch = true;
    private static boolean forceCreateWithInitialData = false;

    private static String scriptFolderForTestDB = "../server-application-server/sourceTest/sql/openbis";

    private static String scriptFolderForEmptyDB = "../server-application-server/source/sql/openbis";

    public static void init()
    {
        initWithoutIndex();
    }

    public static void initWithoutIndex()
    {
        init(getScriptFolderTestDB());
    }

    public static void initWithIndex()
    {
        initWithoutIndex();
    }

    public static void initEmptyDbNoIndex()
    {
        init(getScriptFolderEmptyDB());
    }

    public static void initEmptyDbWithIndex()
    {
        initEmptyDbNoIndex();
    }

    private static void init(String scriptFolder)
    {
        LogInitializer.init();

        System.setProperty("database.create-from-scratch",
                String.valueOf(getCreateDBFromScratch()));
        System.setProperty("database.force-create-with-initial-data",
                String.valueOf(getForceCreateWithInitialData()));
        System.setProperty("database.kind", getDBKind());
        System.setProperty("script-folder", scriptFolder);

        System.setProperty("messages-database.kind", getDBKind());

        DBMigrationEngine.deleteFullTextSearchDocumentVersionFile();
    }

    public static boolean getCreateDBFromScratch()
    {
        return createDBFromScratch;
    }

    public static void setCreateDBFromScratch(boolean createDBFromScratch)
    {
        TestInitializer.createDBFromScratch = createDBFromScratch;
    }

    public static boolean getForceCreateWithInitialData()
    {
        return forceCreateWithInitialData;
    }

    public static void setForceCreateWithInitialData(boolean forceCreateWithInitialData)
    {
        TestInitializer.forceCreateWithInitialData = forceCreateWithInitialData;
    }

    public static String getDBKind()
    {
        return dbKind;
    }

    public static void setDBKind(String dbKind)
    {
        TestInitializer.dbKind = dbKind;
    }

    public static void setDBKindForIndexing(String dbKindForIndexing)
    {
    }

    public static String getScriptFolderTestDB()
    {
        return scriptFolderForTestDB;
    }

    public static void setScriptFolderForTestDB(String scriptFolderForTestDB)
    {
        TestInitializer.scriptFolderForTestDB = scriptFolderForTestDB;
    }

    public static String getScriptFolderEmptyDB()
    {
        return scriptFolderForEmptyDB;
    }

    public static void setScriptFolderForEmptyDB(String scriptFolderForEmptyDB)
    {
        TestInitializer.scriptFolderForEmptyDB = scriptFolderForEmptyDB;
    }

}
