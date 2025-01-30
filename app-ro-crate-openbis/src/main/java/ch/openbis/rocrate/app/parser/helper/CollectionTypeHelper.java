package ch.openbis.rocrate.app.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.openbis.rocrate.app.parser.IAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectionTypeHelper extends BasicImportHelper
{

    public static final String EXPERIMENT_TYPE_FIELD = "Experiment type";

    public enum Attribute implements IAttribute
    {
        Version("Version", false, false),
        Code("Code", true, true),
        Description("Description", true, false),
        ValidationScript("Validation script", true, false),
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false),
        Internal("Internal", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        @Override
        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    Map<String, ExperimentType> accumulator = new HashMap();

    public CollectionTypeHelper()
    {
        super();
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code =
                getValueByColumnName(header, values, Attribute.Code);
        String description = getValueByColumnName(header, values,
                Attribute.Description);
        String validationScript = getValueByColumnName(header, values,
                Attribute.ValidationScript);
        String internal =
                getValueByColumnName(header, values, Attribute.Internal);

        ExperimentType creation = new ExperimentType();
        ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        creation.setFetchOptions(fetchOptions);

        creation.setCode(code);
        creation.setDescription(description);
        creation.setManagedInternally(false);
        creation.setPropertyAssignments(new ArrayList<>());

        accumulator.put(code, creation);

    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {

    }

    @Override
    protected void validateHeader(Map<String, Integer> header)
    {

    }

    public Map<EntityTypePermId, IEntityType> getResult()
    {
        return accumulator.values().stream()
                .collect(Collectors.toMap(
                        x -> new EntityTypePermId(x.getCode(), EntityKind.EXPERIMENT), x -> x));
    }

}
