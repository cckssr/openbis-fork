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

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.log.standard.utils.LogInitializer;
import ch.systemsx.cisd.dbmigration.DBMigrationEngine;

import java.util.List;

/**
 * @author Franz-Josef Elmer
 */
public class TestInitializer
{
    static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            TestInitializer.class);

    private static final String dbKind = "test";

    private static boolean createDBFromScratch = true;

    private static boolean forceCreateWithInitialData = false;

    private static final String testOpenBISDBScriptFolder = "../server-application-server/sourceTest/sql/openbis";

    private static final String testMessagesDBScriptFolder = "../server-application-server/sourceTest/sql/messages";

    private static final String emptyOpenBISDBScriptFolder = "../server-application-server/source/sql/openbis";

    private static final String emptyMessagesDBScriptFolder = "../server-application-server/source/sql/messages";

    private static final String SYSTEM_PROPERTY_PREFIX = "as.";

    public static void init()
    {
        init("");
    }

    public static void init(String systemPropertyPrefix)
    {
        init(testOpenBISDBScriptFolder, testMessagesDBScriptFolder, systemPropertyPrefix);
    }

    public static void initEmptyDb()
    {
        init(emptyOpenBISDBScriptFolder, emptyMessagesDBScriptFolder, SYSTEM_PROPERTY_PREFIX);
    }

    public static void initEmptyDb(String systemPropertyPrefix)
    {
        init(emptyOpenBISDBScriptFolder, emptyMessagesDBScriptFolder, systemPropertyPrefix);
    }

    private static void init(String openBISDBScriptFolder, String messagesDBScriptFolder, String systemPropertyPrefix)
    {
        LogInitializer.init();


        System.setProperty(systemPropertyPrefix+"database.create-from-scratch",
                String.valueOf(getCreateDBFromScratch()));
        System.setProperty(systemPropertyPrefix+"database.force-create-with-initial-data",
                String.valueOf(getForceCreateWithInitialData()));
        System.setProperty(systemPropertyPrefix+"database.kind", dbKind);
        System.setProperty(systemPropertyPrefix+"script-folder", openBISDBScriptFolder);

        System.setProperty(systemPropertyPrefix+"messages-database.kind", dbKind);
        System.setProperty(systemPropertyPrefix+"messages-database.script-folder", messagesDBScriptFolder);

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

}
