package ch.ethz.sis.tools.referencecheck;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;

import java.util.*;
import java.util.stream.Collectors;

public class OpenBisCheck
{

    public static List<CheckResult> checkResultList(String url, String token, String space,
            String project, List<ReferenceCheck.StatementToFix> statementToFixList)
    {
        List<CheckResult> res = new ArrayList<>();
        List<String> identifiers =
                statementToFixList.stream().map(x -> mapIdentifier(x.objectName(), space, project))
                        .toList();

        Map<String, String> identifierToReference = new LinkedHashMap<>();
        OpenBIS openBis = new OpenBIS(url);
        openBis.setSessionToken(token);
        if (!openBis.isSessionActive())
        {
            throw new RuntimeException("openBIS not connected!");
        }

        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
        sampleSearchCriteria.withOrOperator();
        for (ReferenceCheck.StatementToFix statementToFix : statementToFixList)
        {
            String identifier = mapIdentifier(statementToFix.objectName(), space, project);
            sampleSearchCriteria.withIdentifier().thatEquals(identifier);
            identifierToReference.put(identifier, statementToFix.objectName());
        }
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();
        sampleFetchOptions.withType().withPropertyAssignments();

        SearchResult<Sample> sampleSearchResult =
                openBis.searchSamples(sampleSearchCriteria, sampleFetchOptions);
        Set<String> sampleIdentifiers =
                sampleSearchResult.getObjects().stream().map(x -> x.getIdentifier())
                        .map(x -> x.getIdentifier())
                        .collect(Collectors.toSet());
        for (Map.Entry<String, String> identifierToSample : identifierToReference.entrySet())
        {
            boolean found = sampleIdentifiers.contains(identifierToSample.getKey());
            CheckResult checkResult =
                    new CheckResult(identifierToSample.getKey(), identifierToSample.getValue(),
                            found);
            res.add(checkResult);

        }
        return res;

    }

    record CheckResult(String nameUri, String identifier, boolean found)
    {

    }

    static String mapIdentifier(String resourceLocalName, String space, String project)
    {
        return "/" + space + "/" + project + "/" + OpenBisModel.makeOpenBisCodeCompliant(
                resourceLocalName.toUpperCase(Locale.ROOT));
    }

}
