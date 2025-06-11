package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;

import java.util.Optional;

public class RoCrateValidate
{

    public static boolean validate(OpenBisModel openBisModel)
    {
        Optional<SampleType> publication =
                openBisModel.getEntityTypes().values().stream().filter(x -> x instanceof SampleType)
                        .map(SampleType.class::cast)
                        .filter(x -> x.getSemanticAnnotations() != null)
                        .filter(x -> x.getSemanticAnnotations().stream().anyMatch(
                                y -> y.getDescriptorAccessionId()
                                        .equals("https://schema.org/CreativeWork")))
                        .findFirst();

        if (publication.isEmpty())
        {
            return false;
        }
        Optional<PropertyAssignment>
                maybeCreator = publication.get().getPropertyAssignments().stream()
                .filter(x -> x.getSemanticAnnotations() != null)
                .filter(x -> x.getSemanticAnnotations().stream().anyMatch(
                        y -> y.getPredicateAccessionId().equals("https://schema.org/creator")))
                .findFirst();
        if (maybeCreator.isEmpty())
        {
            return false;
        }

        return true;
    }

    private boolean validateCreator()
    {
        return true;
    }

}
