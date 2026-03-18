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
package ch.systemsx.cisd.openbis.plugin.screening.server.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.lemnik.eodsql.DataIterator;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.time.StopWatch;

import ch.systemsx.cisd.openbis.generic.server.business.bo.samplelister.ISampleLister;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.CodeAndLabel;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ListOrSearchSampleCriteria;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import ch.systemsx.cisd.openbis.plugin.screening.server.IScreeningBusinessObjectFactory;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.BasicWellContentQueryResult;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.IScreeningQuery;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.IWellReference;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.PatternMatchingUtils;
import ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess.WellContentQueryResult;
import ch.systemsx.cisd.openbis.plugin.screening.server.logic.dto.IWellData;
import ch.systemsx.cisd.openbis.plugin.screening.server.logic.dto.WellData;
import ch.systemsx.cisd.openbis.plugin.screening.server.logic.dto.WellExtendedData;
import ch.systemsx.cisd.openbis.plugin.screening.shared.api.v1.dto.PlateIdentifier;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ExperimentReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.FeatureVectorValues;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellReference;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.WellSearchCriteria.AnalysisProcedureCriteria;
import ch.systemsx.cisd.openbis.plugin.screening.shared.imaging.FeatureVectorLoader.WellFeatureCollection;

/**
 * Loads well's feature vectors and materials and calculates summaries.
 * 
 * @author Tomasz Pylak
 */
class WellDataLoader extends AbstractContentLoader
{

    public WellDataLoader(Session session, IScreeningBusinessObjectFactory businessObjectFactory,
            IDAOFactory daoFactory, IScreeningQuery screeningQuery)
    {
        super(session, businessObjectFactory, daoFactory, screeningQuery);
    }

    private Map<WellReference, Sample> loadEnrichedWellSamples(
            Iterable<WellContentQueryResult> wells)
    {
        Set<Long> wellIds = extractWellIds(wells);
        List<Sample> wellSamples = loadSamplesWithMaterialPropertiesEnriched(wellIds);
        return asWellRefToSampleMap(wellSamples, wells);
    }


    private static Map<WellReference, Sample> asWellRefToSampleMap(List<Sample> wellSamples,
            Iterable<WellContentQueryResult> wells)
    {
        Map<WellReference, Sample> wellRefToSampleMap = new HashMap<WellReference, Sample>();
        Map<Long, Sample> idToSampleMap = asSampleIdMap(wellSamples);
        for (WellContentQueryResult well : wells)
        {
            Sample sample = idToSampleMap.get(well.well_id);
            wellRefToSampleMap.put(well.getWellReference(), sample);
        }
        return wellRefToSampleMap;
    }

    private static Map<Long, Sample> asSampleIdMap(List<Sample> samples)
    {
        Map<Long/* sample id */, Sample> map = new HashMap<Long, Sample>();
        for (Sample sample : samples)
        {
            map.put(sample.getId(), sample);
        }
        return map;
    }

    private List<Sample> loadSamplesWithMaterialPropertiesEnriched(Set<Long> wellIds)
    {
        throw new IllegalStateException("Material removed!");
    }

    private static Set<Long> extractWellIds(Iterable<WellContentQueryResult> wells)
    {
        Set<Long> ids = new HashSet<Long>();
        for (WellContentQueryResult well : wells)
        {
            ids.add(well.well_id);
        }
        return ids;
    }



}
