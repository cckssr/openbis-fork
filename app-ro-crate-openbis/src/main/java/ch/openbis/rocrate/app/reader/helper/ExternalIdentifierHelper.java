package ch.openbis.rocrate.app.reader.helper;

import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Optional;

public class ExternalIdentifierHelper
{
    public static final String CODE = "RO-Crate.ID";

    public static void setAdditionalPropertyTypes(SampleType sampleType)
    {
        Optional<PropertyAssignment> maybeExisting = sampleType.getPropertyAssignments().stream()
                .filter(x -> x.getPropertyType().getCode().equals(CODE)).findFirst();
        if (maybeExisting.isPresent())
        {
            return;
        }
        PropertyType propertyType = getPropertyType();
        PropertyAssignment propertyAssignment =
                getPropertyAssignment(propertyType);
        propertyAssignment.setEntityType(sampleType);

        sampleType.getPropertyAssignments().add(propertyAssignment);

    }

    private static @NonNull PropertyAssignment getPropertyAssignment(PropertyType propertyType)
    {
        PropertyAssignment propertyAssignment = new PropertyAssignment();
        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        fetchOptions.withEntityType();
        fetchOptions.withSemanticAnnotations();

        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setMandatory(false);
        propertyAssignment.setSemanticAnnotations(new ArrayList<>());

        propertyAssignment.setPropertyType(propertyType);
        return propertyAssignment;
    }

    private static @NonNull PropertyType getPropertyType()
    {
        PropertyType propertyType = new PropertyType();
        propertyType.setSemanticAnnotations(new ArrayList<>());
        propertyType.setCode(CODE);
        propertyType.setDataType(DataType.VARCHAR);
        propertyType.setDescription("External identifier from RO-Crate");
        propertyType.setMultiValue(false);
        propertyType.setLabel("RO-Crate ID");
        {
            PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
            fetchOptions.withSemanticAnnotations();
            fetchOptions.withSampleType();
            fetchOptions.withVocabulary();
            propertyType.setFetchOptions(fetchOptions);
        }
        return propertyType;
    }

    public static void setAdditionalPropertyTypes(ExperimentType experimentType)
    {
        Optional<PropertyAssignment> maybeExisting =
                experimentType.getPropertyAssignments().stream()
                        .filter(x -> x.getPropertyType().getCode().equals(CODE)).findFirst();
        if (maybeExisting.isPresent())
        {
            return;
        }
        PropertyType propertyType = getPropertyType();
        PropertyAssignment propertyAssignment = new PropertyAssignment();
        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        fetchOptions.withEntityType();
        fetchOptions.withSemanticAnnotations();
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        propertyAssignment.setEntityType(experimentType);
        propertyAssignment.setMandatory(false);
        propertyAssignment.setSemanticAnnotations(new ArrayList<>());

        experimentType.getPropertyAssignments().add(propertyAssignment);

    }

    public static void setProperty(AbstractEntityPropertyHolder holder, IMetadataEntry entry)
    {
        holder.getProperties().put(CODE, entry.getId());
    }

}
