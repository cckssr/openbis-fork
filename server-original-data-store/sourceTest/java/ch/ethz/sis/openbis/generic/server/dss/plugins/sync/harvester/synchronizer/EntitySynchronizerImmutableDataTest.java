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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.testng.annotations.Test;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewExperiment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewSample;

public class EntitySynchronizerImmutableDataTest
{
    @Test
    public void testCollectsOnlySamplesWithImmutableData()
    {
        IncomingSample frozen = sample("frozen", new Date(1000));
        IncomingSample mutable = sample("mutable", null);

        List<ImmutableDataTimestamp> timestamps = EntitySynchronizer.collectImmutableDataTimestamps(List.of(frozen, mutable));

        assertEquals(timestamps.size(), 1);
        assertEquals(timestamps.get(0).permId, "frozen");
        assertEquals(timestamps.get(0).immutableDataTimestamp, new Date(1000));
    }

    @Test
    public void testCollectsOnlyExperimentsWithImmutableData()
    {
        IncomingExperiment mutable = experiment("mutable", null);
        IncomingExperiment frozen = experiment("frozen", new Date(2000));

        List<ImmutableDataTimestamp> timestamps = EntitySynchronizer.collectImmutableDataTimestamps(List.of(mutable, frozen));

        assertEquals(timestamps.size(), 1);
        assertEquals(timestamps.get(0).permId, "frozen");
        assertEquals(timestamps.get(0).immutableDataTimestamp, new Date(2000));
    }

    @Test
    public void testCollectsNothingWhenNoEntityHasImmutableData()
    {
        assertTrue(EntitySynchronizer.collectImmutableDataTimestamps(List.of(sample("mutable", null))).isEmpty());
        assertTrue(EntitySynchronizer.collectImmutableDataTimestamps(List.of()).isEmpty());
    }

    private IncomingSample sample(String permId, Date immutableDataDate)
    {
        NewSample sample = new NewSample();
        sample.setPermID(permId);
        IncomingSample incomingSample = new IncomingSample(sample, new FrozenFlags(permId, false), new Date(1));
        incomingSample.setImmutableDataDate(immutableDataDate);
        return incomingSample;
    }

    private IncomingExperiment experiment(String permId, Date immutableDataDate)
    {
        NewExperiment experiment = new NewExperiment();
        experiment.setPermID(permId);
        IncomingExperiment incomingExperiment = new IncomingExperiment(experiment, new FrozenFlags(permId, false), new Date(1));
        incomingExperiment.setImmutableDataDate(immutableDataDate);
        return incomingExperiment;
    }
}
