package ch.ethz.sis.tools.importcheck.helper;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;

import java.util.ArrayList;
import java.util.List;

public class Connection
{

    public static CheckResult check(OpenBIS openBIS, List<Sample> samples)
    {

        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withOrOperator();
        for (Sample sample : samples)
        {
            criteria.withIdentifier().thatEquals(sample.getIdentifier().toString());
        }
        List<Sample> found = new ArrayList<>();
        List<Sample> missing = new ArrayList<>();

        SearchResult<Sample> result =
                openBIS.searchSamples(criteria, new SampleFetchOptions());
        for (Sample sample : samples)
        {
            if (result.getObjects().stream()
                    .noneMatch(x -> x.getIdentifier().equals(sample.getIdentifier())))
            {
                missing.add(sample);
            } else
            {
                found.add(sample);
            }

        }
        return new CheckResult(found, missing);

    }

    public static class CheckResult
    {
        List<Sample> found;

        List<Sample> missing;

        public CheckResult(List<Sample> found, List<Sample> missing)
        {
            this.found = found;
            this.missing = missing;
        }

        public List<Sample> getFound()
        {
            return found;
        }

        public List<Sample> getMissing()
        {
            return missing;
        }
    }

}
