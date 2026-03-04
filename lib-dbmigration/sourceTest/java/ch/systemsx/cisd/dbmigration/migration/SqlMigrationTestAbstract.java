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
package ch.systemsx.cisd.dbmigration.migration;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;

import ch.ethz.sis.shared.log.standard.utils.LogInitializer;
import ch.systemsx.cisd.dbmigration.DBMigrationEngine;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;
import ch.systemsx.cisd.dbmigration.postgresql.DumpPreparator;


import static ch.systemsx.cisd.dbmigration.DBMigrationEngine.FULL_TEXT_SEARCH_DOCUMENT_VERSION_FILE_PATH;

/**
 * Test cases for database migration.
 *
 * @author Piotr Kupczyk
 */
public abstract class SqlMigrationTestAbstract
{

    private static final int CHECK_NUMBER_OF_MIGRATIONS = 30;

    private File sqlScriptOutputDirectory;

    protected abstract String getSqlScriptInputDirectory();

    protected abstract String getSqlScriptOutputDirectory();

    @BeforeClass(alwaysRun = true)
    public void beforeClass() throws Exception
    {
        LogInitializer.init();
    }

    @BeforeTest(alwaysRun = true)
    public void beforeTest() throws Exception
    {
        sqlScriptOutputDirectory = new File(getSqlScriptOutputDirectory());
        if (!sqlScriptOutputDirectory.exists())
        {
            sqlScriptOutputDirectory.mkdir();
        }
    }

    @AfterTest(alwaysRun = true)
    public void afterTest() throws Exception
    {
        if (sqlScriptOutputDirectory != null && sqlScriptOutputDirectory.exists())
        {
            FileUtils.deleteDirectory(sqlScriptOutputDirectory);
        }
    }

    public void testMigration(final String newestVersionString, final String newestFullTextSearchVersionString, final String newestPatchesVersionString)
    {
        //new File(FULL_TEXT_SEARCH_DOCUMENT_VERSION_FILE_PATH).delete();

        SqlMigrationVersion newestVersion = new SqlMigrationVersion(newestVersionString);
        SqlMigrationVersion firstVersion =
                new SqlMigrationVersion(Math.max(1, newestVersion.getVersionInt() - CHECK_NUMBER_OF_MIGRATIONS));

        DatabaseConfigurationContext migrationContext = null;
        DatabaseConfigurationContext scratchContext = null;

        try
        {
            // create first version of the migration database
            migrationContext = createMigrationDatabaseContext(true);
            DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(migrationContext,
                    firstVersion.getVersionString(), newestFullTextSearchVersionString, newestPatchesVersionString);

            // migrate the migration database to the newest version
            migrationContext.setCreateFromScratch(false);
            DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(migrationContext,
                    newestVersion.getVersionString(), newestFullTextSearchVersionString, newestPatchesVersionString);
            dumpDatabaseSchema(migrationContext, getMigratedDatabaseSchemaFile());

            // create the scratch database with the newest version
            scratchContext = createScratchDatabaseContext();
            DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(scratchContext,
                    newestVersion.getVersionString(), newestFullTextSearchVersionString, newestPatchesVersionString);
            dumpDatabaseSchema(scratchContext, getScratchDatabaseSchemaFile());

            // check migration and scratch databases are equal
            assertDatabaseSchemasEqual(getMigratedDatabaseSchemaFile(),
                    getScratchDatabaseSchemaFile());

        } finally
        {
            if (migrationContext != null)
            {
                migrationContext.closeConnections();
            }
            if (scratchContext != null)
            {
                scratchContext.closeConnections();
            }
        }
    }

    private DatabaseConfigurationContext createDatabaseContext(String dbKind,
            boolean createFromScratch)
    {
        DatabaseConfigurationContext context = new DatabaseConfigurationContext();
        context.setDatabaseEngineCode("postgresql");
        context.setBasicDatabaseName("openbis");
        context.setDatabaseKind(dbKind);
        context.setScriptFolder(getSqlScriptInputDirectory());
        context.initDataSourceFactory(new SqlMigrationDataSourceFactory());
        context.setCreateFromScratch(createFromScratch);
        return context;
    }

    private DatabaseConfigurationContext createMigrationDatabaseContext(boolean createFromScratch)
    {
        return createDatabaseContext("test_migration_migrated", createFromScratch);
    }

    private DatabaseConfigurationContext createScratchDatabaseContext()
    {
        return createDatabaseContext("test_migration_scratch", true);
    }

    private File getMigratedDatabaseSchemaFile()
    {
        return new File(sqlScriptOutputDirectory, "migratedDatabaseSchema.sql");
    }

    private File getScratchDatabaseSchemaFile()
    {
        return new File(sqlScriptOutputDirectory, "scratchDatabaseSchema.sql");
    }

    private void dumpDatabaseSchema(final DatabaseConfigurationContext configurationContext,
            final File migratedSchemaFile)
    {
        final boolean dumpSuccessful =
                DumpPreparator.createDatabaseSchemaDump(configurationContext.getDatabaseName(),
                        migratedSchemaFile);
        AssertJUnit.assertTrue("dump of db failed: " + configurationContext.getDatabaseName(),
                dumpSuccessful);
    }



    private void assertDatabaseSchemasEqual(File migrated, File scratch)
    {
        try
        {
            List<String> m = normalizedSchema(migrated);
            List<String> s = normalizedSchema(scratch);

            List<String> onlyInScratch = new ArrayList<>(s);
            onlyInScratch.removeAll(m);

            List<String> onlyInMigrated = new ArrayList<>(m);
            onlyInMigrated.removeAll(s);

            if (!onlyInScratch.isEmpty() || !onlyInMigrated.isEmpty())
            {
                StringBuilder diff = new StringBuilder();

                diff.append("\n--- Only in SCRATCH ---\n");
                onlyInScratch.forEach(l -> diff.append(l).append("\n"));

                diff.append("\n--- Only in MIGRATED ---\n");
                onlyInMigrated.forEach(l -> diff.append(l).append("\n"));

                AssertJUnit.fail("Schema mismatch between migrated and scratch database:\n" + diff);
            }

            AssertJUnit.assertEquals(
                    "Schema mismatch between migrated and scratch database",
                    s,
                    m
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

//    private void assertDatabaseSchemasEqual(File migrated, File scratch)
//    {
//        try
//        {
//            List<String> m = normalizedSchema(migrated);
//            List<String> s = normalizedSchema(scratch);
//
//            AssertJUnit.assertEquals(
//                    "Schema mismatch between migrated and scratch database",
//                    s,
//                    m
//            );
//        }
//        catch (Exception e)
//        {
//            throw new RuntimeException(e);
//        }
//    }
    private List<String> normalizedSchema(File f) throws Exception
    {
        List<String> lines = FileUtils.readLines(f, "UTF-8");
        List<String> out = new ArrayList<>();

        boolean inCreateTable = false;
        StringBuilder tableBuffer = new StringBuilder();

        for (String raw : lines)
        {
            String l = raw.trim();

            // --- skip noise ---
            if (skipNoise(l))
            {
                continue;
            }

            // normalize whitespace
            l = l.replaceAll("\\s+", " ").trim();

            // normalize inline NOT NULL constraint names
            l = l.replaceAll(
                    "\\s+CONSTRAINT\\s+\\S+\\s+NOT NULL",
                    " NOT NULL"
            );

            // --- CREATE TABLE handling ---
            if (l.startsWith("CREATE TABLE "))
            {
                inCreateTable = true;
                tableBuffer.setLength(0);
                tableBuffer.append(l);
                continue;
            }

            if (inCreateTable)
            {
                tableBuffer.append(" ").append(l);

                if (l.endsWith(");"))
                {
                    out.add(normalizeCreateTable(tableBuffer.toString()));
                    inCreateTable = false;
                }
                continue;
            }

            // everything else
            out.add(l);
        }

        return out;
    }

    private String normalizeCreateTable(String stmt)
    {
        // Locate the outer parentheses of the CREATE TABLE statement
        // Everything before '(' is the table header, everything inside is the definition
        int open = stmt.indexOf('(');
        int close = stmt.lastIndexOf(')');

        // Header includes "CREATE TABLE ... ("
        String header = stmt.substring(0, open + 1);

        // Body contains column definitions and table-level constraints
        String body = stmt.substring(open + 1, close);

        // Split individual definitions by comma
        // (assumes no commas inside column types or expressions)
        String[] parts = body.split(",");

        // Columns and constraints are collected separately so they can be sorted independently
        List<String> columns = new ArrayList<>();
        List<String> constraints = new ArrayList<>();

        for (String p : parts)
        {
            // Trim whitespace around each definition
            String e = p.trim();

            // Ignore empty fragments caused by formatting or trailing commas
            if (e.isEmpty())
            {
                continue;
            }

            // Table-level constraints (named constraints or CHECK constraints)
            if (e.startsWith("CONSTRAINT") || e.startsWith("CHECK"))
            {
                constraints.add(e);
            }
            // Everything else is treated as a column definition
            else
            {
                columns.add(e);
            }
        }

        // Sort columns and constraints to produce deterministic output
        // (order-independent schema comparison)
        columns.sort(String::compareTo);
        constraints.sort(String::compareTo);

        // Rebuild the CREATE TABLE statement in normalized form
        StringBuilder rebuilt = new StringBuilder();
        rebuilt.append(header).append("\n");

        // Emit sorted column definitions first
        for (String c : columns)
        {
            rebuilt.append("  ").append(c).append(",\n");
        }

        // Emit sorted table-level constraints after columns
        for (String c : constraints)
        {
            rebuilt.append("  ").append(c).append(",\n");
        }

        // Remove the trailing comma from the last definition
        int lastComma = rebuilt.lastIndexOf(",");
        if (lastComma != -1)
        {
            rebuilt.deleteCharAt(lastComma);
        }

        // Close the CREATE TABLE statement
        rebuilt.append("\n);");

        return rebuilt.toString();
    }



    private static boolean skipNoise(String l)
    {
        if (l.isEmpty())
        {
            return true;
        }

        // Skip single-line SQL comments (e.g. -- this is a comment)
        if (l.startsWith("--"))
        {
            return true;
        }

        // Skip block comment starts (/* ... */), usually schema metadata
        if (l.startsWith("/*"))
        {
            return true;
        }

        // Skip session-level configuration commands that don't affect schema
        if (l.startsWith("SET "))
        {
            return true;
        }

        // Skip PostgreSQL-specific runtime config restoration commands
        // (commonly emitted by pg_dump, irrelevant for schema comparison)
        if (l.startsWith("SELECT pg_catalog.set_config"))
        {
            return true;
        }

        // Skip COMMENT statements; comments are non-structural metadata
        if (l.startsWith("COMMENT ON"))
        {
            return true;
        }

        // Skip privilege grants; permissions are not part of schema structure
        if (l.startsWith("GRANT "))
        {
            return true;
        }

        // Skip privilege revocations; also non-structural
        if (l.startsWith("REVOKE "))
        {
            return true;
        }

        // Skip pg_dump meta-commands that restrict object creation
        // (used during restore, not actual SQL)
        if (l.startsWith("\\restrict"))
        {
            return true;
        }

        // Skip pg_dump meta-commands that lift restrictions
        return l.startsWith("\\unrestrict");
    }

}
