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
package ch.systemsx.cisd.dbmigration.java;

import javax.sql.DataSource;

import org.testng.AssertJUnit;

import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;

/**
 * A <code>IMigrationStep</code> implementation for test.
 *
 * @author Izabela Adamczyk
 */
public final class MigrationStepFrom002To003 implements IMigrationStep
{
    public MigrationStepFrom002To003(DatabaseConfigurationContext context)
    {
        AssertJUnit.assertNotNull(context);
    }

    //
    // IMigrationStep
    //

    @Override
    public final void performPostMigration(DataSource dataSource)
    {
        throw new RuntimeException("EmptyResultDataAccessException");
    }

    @Override
    public final void performPreMigration(DataSource dataSource)
    {
        throw new RuntimeException("DataIntegrityViolationException");
    }

}
