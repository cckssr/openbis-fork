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

import java.util.List;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.translator.DefaultNameTranslator;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewSpace;

/**
 * Regression test for a space explicitly selected via {@code exportable-perm-ids = SPACE:&lt;code&gt;} being silently
 * dropped by {@link ResourceListParserData#getRelevantSpacesToProcess()} when it has no projects/samples below it
 * (or {@code with-levels-below = false} prevents them from being fetched).
 * <p>
 * {@link ResourceListParser} only adds a space's code to {@code harvesterSpaceList} as a side effect of resolving a
 * project/experiment/sample that belongs to it (see {@code ResourceListParser.createSpaceIdentifier}). A space that
 * was delivered on its own - because it was explicitly selected as an exportable and has no children, or its
 * children were not fetched because {@code with-levels-below = false} - is present in {@code spacesToProcess} but
 * never referenced by any child entity, so it never enters {@code harvesterSpaceList} and is filtered out of
 * {@code getRelevantSpacesToProcess()}. This means the space is neither created nor updated, and no trace of it
 * appears in the harvester log.
 */
public class ResourceListParserDataTest
{
    @Test
    public void testExplicitlySelectedEmptySpaceIsRelevantEvenWithoutChildEntities()
    {
        ResourceListParserData data = new ResourceListParserData(new DefaultNameTranslator());
        IncomingSpace space = space("SPACE1");
        data.getSpacesToProcess().add(space);

        // no project/experiment/sample referencing SPACE1 was parsed, so harvesterSpaceList stays empty -
        // this is exactly what happens for an empty space, or any space when with-levels-below = false
        assertTrue(data.getHarvesterSpaceList().isEmpty());

        List<IncomingSpace> relevantSpaces = data.getRelevantSpacesToProcess();

        assertEquals(relevantSpaces.stream().map(IncomingSpace::getPermID).toList(), List.of("SPACE1"));
    }

    @Test
    public void testSpaceReferencedByAChildEntityIsRelevant()
    {
        ResourceListParserData data = new ResourceListParserData(new DefaultNameTranslator());
        IncomingSpace space = space("SPACE1");
        data.getSpacesToProcess().add(space);

        // simulates with-levels-below = true pulling in a project/sample below SPACE1, which is what
        // ResourceListParser.createSpaceIdentifier populates harvesterSpaceList with as a side effect
        data.getHarvesterSpaceList().add("SPACE1");

        List<IncomingSpace> relevantSpaces = data.getRelevantSpacesToProcess();

        assertEquals(relevantSpaces.stream().map(IncomingSpace::getPermID).toList(), List.of("SPACE1"));
    }

    private IncomingSpace space(String code)
    {
        NewSpace space = new NewSpace(code, null, null);
        return new IncomingSpace(space, new FrozenFlags(code, false), null);
    }
}
