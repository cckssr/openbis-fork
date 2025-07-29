package ch.ethz.sis.rocrateserver.openapi.v1.service.helper;

import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.PropertyError;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.ValidationError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationErrorMapping
{
    public static List<ValidationError> mapErrors(
            RoCrateSchemaValidation.ValidationResult validationResult)
    {

        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validationResult.getEntitiesToMissingProperties().values().stream()
                .flatMap(Collection::stream)
                .map(x -> new PropertyError(x.node, x.property, x.message))
                .collect(Collectors.toList()));
        errors.addAll(
                validationResult.getWrongDataTypes().values().stream().flatMap(Collection::stream)
                        .map(x -> new PropertyError(x.node, x.property, x.message))
                        .collect(Collectors.toList()));

        errors.addAll(validationResult.getEntititesToUndefinedProperties().values().stream()
                .flatMap(Collection::stream)
                .map(x -> new PropertyError(x.node, x.property, x.message))
                .collect(Collectors.toList()));
        return errors;

    }

}
