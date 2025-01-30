package ch.openbis.rocrate.app.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.openbis.rocrate.app.parser.IAttribute;
import ch.openbis.rocrate.app.parser.searcher.AttributeValidator;
import ch.openbis.rocrate.app.parser.searcher.PropertyTypeSearcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.openbis.rocrate.app.parser.helper.CollectionTypeHelper.EXPERIMENT_TYPE_FIELD;
import static ch.openbis.rocrate.app.parser.searcher.PropertyTypeSearcher.getPropertyValue;

public class CollectionHelper extends BasicImportHelper
{

    public enum Attribute implements IAttribute
    {
        Identifier("Identifier", false, true),
        Code("Code", true, true),
        Project("Project", true, true);

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

    Map<String, Experiment> accumulator = new HashMap<>();

    Map<String, Experiment> usageCode = new HashMap<>();

    Map<String, String> codeToSpace = new HashMap<>();

    Map<String, String> codeToProject = new HashMap<>();

    CollectionTypeHelper collectionTypeHelper;

    private final AttributeValidator<Attribute> attributeValidator;

    ProjectHelper projectHelper;

    private PropertyTypeSearcher propertyTypeSearcher;

    private EntityTypePermId experimentType;

    public CollectionHelper(CollectionTypeHelper collectionTypeHelper,
            ProjectHelper projectHelper)
    {
        this.collectionTypeHelper = collectionTypeHelper;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
        this.projectHelper = projectHelper;

    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return false;
    }

    @Override
    public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {

        int lineIndex = start;

        try
        {
            Map<String, Integer> header = parseHeader(page.get(lineIndex), false);
            AttributeValidator.validateHeader(EXPERIMENT_TYPE_FIELD, header);
            lineIndex++;

            experimentType = new EntityTypePermId(
                    getValueByColumnName(header, page.get(lineIndex), EXPERIMENT_TYPE_FIELD),
                    EntityKind.EXPERIMENT);
            if (experimentType.getPermId() == null || experimentType.getPermId().isEmpty())
            {
                throw new UserFailureException(
                        "Mandatory field is missing or empty: " + EXPERIMENT_TYPE_FIELD);
            }

            // first check that experiment type exist.
            ExperimentTypeFetchOptions fetchTypeOptions = new ExperimentTypeFetchOptions();
            fetchTypeOptions.withPropertyAssignments().withPropertyType().withVocabulary()
                    .withTerms();
            ExperimentType type =
                    (ExperimentType) collectionTypeHelper.getResult().get(experimentType);
            if (type == null)
            {
                throw new UserFailureException("Experiment type " + experimentType + " not found.");
            }
            this.propertyTypeSearcher = new PropertyTypeSearcher(type.getPropertyAssignments());

            lineIndex++;
        } catch (Exception e)
        {
            throw new UserFailureException(
                    "sheet: " + (pageIndex + 1) + " line: " + (lineIndex + 1) + " message: " + e.getMessage());
        }

        super.importBlock(page, pageIndex, lineIndex, end);
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        Experiment exerpiment = new Experiment();
        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withProject();
        experimentFetchOptions.withType();
        experimentFetchOptions.withProperties();
        exerpiment.setFetchOptions(experimentFetchOptions);

        String code = getValueByColumnName(header, values, Attribute.Code);
        String project =
                getValueByColumnName(header, values, Attribute.Project);

        ExperimentType collectionType =
                (ExperimentType) collectionTypeHelper.getResult().get(experimentType);
        String identifier =
                getValueByColumnName(header, values, Attribute.Identifier);

        Project projectForReal = projectHelper.getProject(project);

        exerpiment.setType(collectionType);
        exerpiment.setCode(code);
        exerpiment.setProject(projectForReal);
        exerpiment.setIdentifier(new ExperimentIdentifier(identifier));
        projectForReal.getSpace();

        for (String key : header.keySet())
        {
            if (!attributeValidator.isHeader(key))
            {
                String value = getValueByColumnName(header, values, key);
                PropertyType propertyType = propertyTypeSearcher.findPropertyType(key);
                exerpiment.setProperty(propertyType.getCode(),
                        getPropertyValue(propertyType, value));
            }
        }

        accumulator.put(code, exerpiment);
        usageCode.put(project + "/" + code, exerpiment);
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

    Experiment getByUsageCode(String code)
    {
        return usageCode.get(code);

    }

    public Map<ObjectIdentifier, AbstractEntity> getResult()
    {
        return accumulator.values().stream().collect(Collectors.toMap(
                x -> new ExperimentIdentifier(x.getProject().getCode(),
                        x.getProject().getSpace().getCode(), x.getCode()), x -> x));
    }

}
