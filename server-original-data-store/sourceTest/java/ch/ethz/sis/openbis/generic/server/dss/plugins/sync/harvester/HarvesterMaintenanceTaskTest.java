/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.EntitySynchronizer;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.SynchronizationContext;

public class HarvesterMaintenanceTaskTest
{
    @Test
    public void testRunsAfsAfterEntitySynchronization() throws Exception
    {
        List<String> phases = new ArrayList<>();
        Date resourceListTimestamp = new Date(1234L);
        EntitySynchronizer synchronizer = new EntitySynchronizer(new SynchronizationContext())
        {
            @Override
            public Date synchronizeEntities()
            {
                phases.add("entities");
                return resourceListTimestamp;
            }

            @Override
            public void synchronizeAFSData()
            {
                phases.add("afs");
            }
        };

        Date actualTimestamp = HarvesterMaintenanceTask.synchronizePhases(synchronizer, false);

        assertEquals(actualTimestamp, resourceListTimestamp);
        assertEquals(phases, List.of("entities", "afs"));
    }

    @Test
    public void testDryRunDoesNotRunAfsSynchronization() throws Exception
    {
        List<String> phases = new ArrayList<>();
        Date resourceListTimestamp = new Date(1234L);
        EntitySynchronizer synchronizer = new EntitySynchronizer(new SynchronizationContext())
        {
            @Override
            public Date synchronizeEntities()
            {
                phases.add("entities");
                return resourceListTimestamp;
            }

            @Override
            public void synchronizeAFSData()
            {
                phases.add("afs");
            }
        };

        Date actualTimestamp = HarvesterMaintenanceTask.synchronizePhases(synchronizer, true);

        assertEquals(actualTimestamp, resourceListTimestamp);
        assertEquals(phases, List.of("entities"));
    }

    @Test
    public void testEntitySynchronizationFailurePreventsAfsSynchronization()
    {
        boolean[] afsCalled = { false };
        EntitySynchronizer synchronizer = new EntitySynchronizer(new SynchronizationContext())
        {
            @Override
            public Date synchronizeEntities() throws Exception
            {
                throw new Exception("entity failure");
            }

            @Override
            public void synchronizeAFSData()
            {
                afsCalled[0] = true;
            }
        };

        try
        {
            HarvesterMaintenanceTask.synchronizePhases(synchronizer, false);
            fail("Expected entity synchronization to fail");
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "entity failure");
        }
        assertFalse(afsCalled[0]);
    }

    @Test
    public void testAfsSynchronizationFailureIsPropagated()
    {
        List<String> phases = new ArrayList<>();
        Date resourceListTimestamp = new Date(1234L);
        EntitySynchronizer synchronizer = new EntitySynchronizer(new SynchronizationContext())
        {
            @Override
            public Date synchronizeEntities()
            {
                phases.add("entities");
                return resourceListTimestamp;
            }

            @Override
            public void synchronizeAFSData() throws Exception
            {
                phases.add("afs");
                throw new Exception("AFS failure");
            }
        };

        try
        {
            HarvesterMaintenanceTask.synchronizePhases(synchronizer, false);
            fail("Expected AFS synchronization to fail");
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "AFS failure");
        }
        assertEquals(phases, List.of("entities", "afs"));
    }

    @Test
    public void testAfsSynchronizationRequiresCompletedEntitySynchronization()
    {
        EntitySynchronizer synchronizer = new EntitySynchronizer(new SynchronizationContext());

        try
        {
            synchronizer.synchronizeAFSData();
            fail("Expected AFS synchronization to require completed entity synchronization");
        } catch (Exception e)
        {
            assertEquals(e.getClass(), IllegalStateException.class);
            assertEquals(e.getMessage(), "AFS synchronization requires completed entity synchronization");
        }
    }
}
