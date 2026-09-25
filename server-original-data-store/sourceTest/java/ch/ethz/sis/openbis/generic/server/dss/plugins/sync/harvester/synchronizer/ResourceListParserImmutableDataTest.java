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
import static org.testng.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.Test;
import org.w3c.dom.Document;

import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.translator.DefaultNameTranslator;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.util.Monitor;
import ch.ethz.sis.shared.log.classic.impl.Logger;

/**
 * Freezing an owner on the data source sets its immutable data timestamp, which is what makes its AFS content read-only. The timestamp is delivered
 * as {@code immutable-data-timestamp} on the owner's {@code x:xd} element so the harvester can mirror it.
 */
public class ResourceListParserImmutableDataTest
{
    private static final String FROZEN_SAMPLE = "20260101000000000-1";

    private static final String MUTABLE_SAMPLE = "20260101000000000-2";

    private static final String FROZEN_EXPERIMENT = "20260101000000000-3";

    private static final String MUTABLE_EXPERIMENT = "20260101000000000-4";

    @Test
    public void testParsesImmutableDataTimestampOfSamplesAndExperiments() throws Exception
    {
        String xml = "<urlset xmlns:rs=\"http://www.openarchives.org/rs/terms/\" xmlns:x=\"https://sis.id.ethz.ch/software/#openbis/xdterms/\">"
                + "<rs:md at=\"2026-09-25T10:00:00Z\"/>"
                + url("SAMPLE", FROZEN_SAMPLE, "S1", " immutable-data-timestamp=\"2026-09-24T12:30:45Z\" frozen=\"true\"")
                + url("SAMPLE", MUTABLE_SAMPLE, "S2", "")
                + url("EXPERIMENT", FROZEN_EXPERIMENT, "E1", " immutable-data-timestamp=\"2026-09-23T08:00:00Z\" project=\"P\"")
                + url("EXPERIMENT", MUTABLE_EXPERIMENT, "E2", " project=\"P\"")
                + "</urlset>";

        ResourceListParserData data = ResourceListParser.create(new DefaultNameTranslator(), "DSS1")
                .parseResourceListDocument(parse(xml), new Monitor("test", Logger.getLogger(Monitor.class)));

        assertEquals(data.getSamplesToProcess().get(FROZEN_SAMPLE).getImmutableDataDate(), new Date(1790253045000L));
        assertNull(data.getSamplesToProcess().get(MUTABLE_SAMPLE).getImmutableDataDate());
        assertEquals(data.getExperimentsToProcess().get(FROZEN_EXPERIMENT).getImmutableDataDate(), new Date(1790150400000L));
        assertNull(data.getExperimentsToProcess().get(MUTABLE_EXPERIMENT).getImmutableDataDate());
    }

    private static String url(String kind, String permId, String code, String extraAttributes)
    {
        return "<url><loc>http://localhost/" + kind + "/" + permId + "/M</loc><lastmod>2026-09-24T12:00:00Z</lastmod>"
                + "<x:xd kind=\"" + kind + "\" code=\"" + code + "\" type=\"T\" space=\"SPACE\" registrator=\"admin\""
                + " registration-timestamp=\"2026-01-01T00:00:00Z\"" + extraAttributes + "/></url>";
    }

    private static Document parse(String xml) throws Exception
    {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
