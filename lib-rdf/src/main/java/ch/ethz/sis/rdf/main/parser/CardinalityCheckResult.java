package ch.ethz.sis.rdf.main.parser;

import java.util.Objects;
import java.util.Set;

public class CardinalityCheckResult
{
    public static class ResourceWithValues
    {
        String resourceId;

        String propertyLabel;

        Set<String> values;

        public ResourceWithValues(String resourceId, String propertyLabel, Set<String> values)
        {
            this.resourceId = resourceId;
            this.propertyLabel = propertyLabel;
            this.values = values;
        }

        public String getResourceId()
        {
            return resourceId;
        }

        public String getPropertyLabel()
        {
            return propertyLabel;
        }

        public Set<String> getValues()
        {
            return values;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ResourceWithValues that = (ResourceWithValues) o;
            return Objects.equals(resourceId, that.resourceId) && Objects.equals(
                    propertyLabel, that.propertyLabel);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(resourceId, propertyLabel);
        }
    }

    Set<ResourceWithValues> tooManyValues;

    Set<ResourceWithValues> tooFewValues;

    public CardinalityCheckResult(Set<ResourceWithValues> tooManyValues,
            Set<ResourceWithValues> tooFewValues)
    {

        this.tooManyValues = tooManyValues;
        this.tooFewValues = tooFewValues;
    }
}
