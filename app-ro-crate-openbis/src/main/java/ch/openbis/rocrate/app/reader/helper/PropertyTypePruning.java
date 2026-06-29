package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PropertyTypePruning
{
    public static void prune(Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities)
    {

        Map<SampleType, List<Sample>> typeToSamples =
                entities.values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .collect(Collectors.groupingBy(Sample::getType));

        Map<SampleType, List<PropertyAssignment>> toPrune = new LinkedHashMap<>();

        for (Map.Entry<SampleType, List<Sample>> typeWithSample : typeToSamples.entrySet())
        {

            for (PropertyAssignment propertyAssignment : typeWithSample.getKey()
                    .getPropertyAssignments())
            {
                boolean used;
                String code = propertyAssignment.getPropertyType().getCode();
                used = typeWithSample.getValue().stream()
                        .anyMatch(x -> x.getProperties().containsKey(
                                code));
                if (!used && !propertyAssignment.isMandatory())
                {
                    List<PropertyAssignment> pruneList =
                            toPrune.getOrDefault(typeWithSample.getKey(), new ArrayList<>());
                    pruneList.add(propertyAssignment);
                    toPrune.put(typeWithSample.getKey(), pruneList);

                }

            }
        }

        for (Map.Entry<SampleType, List<PropertyAssignment>> pruneEntry : toPrune.entrySet())
        {
            SampleType type = pruneEntry.getKey();
            List<PropertyAssignment> reaminingPropertyAssignments =
                    type.getPropertyAssignments().stream()
                            .filter(x -> !pruneEntry.getValue().contains(x)).toList();
            pruneEntry.getKey().setPropertyAssignments(reaminingPropertyAssignments);
        }

    }
}
