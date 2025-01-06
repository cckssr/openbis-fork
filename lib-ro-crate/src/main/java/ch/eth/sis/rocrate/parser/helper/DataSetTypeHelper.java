package ch.eth.sis.rocrate.parser.helper;

import ch.eth.sis.rocrate.parser.IAttribute;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataSetTypeHelper extends BasicImportHelper
{

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


    String currentCode = null;

    private Map<String, DataSetType> accumulator = new HashMap<>();

    public DataSetTypeHelper()
    {
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
        String code = getValueByColumnName(header, values, Attribute.Code);
        this.currentCode = code;
        String description =
                getValueByColumnName(header, values, Attribute.Description);
        String validationScript = getValueByColumnName(header, values,
                Attribute.ValidationScript);
        String internal =
                getValueByColumnName(header, values, Attribute.Internal);

        DataSetType type = new DataSetType();
        DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        type.setFetchOptions(fetchOptions);
        type.setPropertyAssignments(new ArrayList<>());
        type.setCode(code);
        type.setDescription(description);

        type.setValidationPlugin(null);
        type.setManagedInternally(false);
        accumulator.put(code, type);

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
                        x -> new EntityTypePermId(x.getCode(), EntityKind.DATA_SET), x -> x));
    }

    public String getCurrentCode()
    {
        return currentCode;
    }
}
