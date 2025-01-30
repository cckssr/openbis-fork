package ch.openbis.rocrate.app.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.openbis.rocrate.app.parser.IAttribute;
import ch.openbis.rocrate.app.parser.searcher.AttributeValidator;
import ch.openbis.rocrate.app.parser.searcher.PropertyTypeSearcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.openbis.rocrate.app.parser.searcher.PropertyTypeSearcher.VARIABLE_PREFIX;
import static ch.openbis.rocrate.app.parser.searcher.PropertyTypeSearcher.getPropertyValue;

public class ObjectHelper extends BasicImportHelper
{

    public static final String SAMPLE_TYPE_FIELD = "Sample type";

    public enum Attribute implements IAttribute
    {
        $("$", false, true),
        Identifier("Identifier", false, true),
        Code("Code", false, true),
        Space("Space", false, true),
        Project("Project", false, true),
        Experiment("Experiment", false, true),
        AutoGenerateCode("Auto generate code", false, false),
        Parents("Parents", false, true),
        Children("Children", false, true);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

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

    ObjectTypeHelper objectTypeHelper;

    private EntityTypePermId sampleType;

    Map<String, Sample> accumulator = new HashMap<>();

    Map<String, List<SampleIdentifier>> childrenToResolve = new HashMap<>();

    Map<String, List<SampleIdentifier>> parentsToResolve = new HashMap<>();

    Map<String, String> collectionsToResolve = new HashMap<>();

    Map<String, Sample> identifierToSample = new HashMap<>();

    SpaceHelper spaceHelper;

    ProjectHelper projectHelper;

    CollectionTypeHelper collectionTypeHelper;

    private final AttributeValidator<Attribute> attributeValidator;

    PropertyTypeSearcher propertyTypeSearcher;

    CollectionHelper collectionHelper;

    public ObjectHelper(SpaceHelper spaceHelper,
            ProjectHelper projectHelper,
            CollectionTypeHelper collectionTypeHelper, ObjectTypeHelper objectTypeHelper,
            CollectionHelper collectionHelper)
    {
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
        this.spaceHelper = spaceHelper;
        this.projectHelper = projectHelper;
        this.collectionTypeHelper = collectionTypeHelper;
        this.objectTypeHelper = objectTypeHelper;
        this.collectionHelper = collectionHelper;

    }

    @Override
    public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        int lineIndex = start;

        try
        {
            Map<String, Integer> header = parseHeader(page.get(lineIndex), false);
            AttributeValidator.validateHeader(SAMPLE_TYPE_FIELD, header);
            lineIndex++;

            sampleType = new EntityTypePermId(
                    getValueByColumnName(header, page.get(lineIndex), SAMPLE_TYPE_FIELD),
                    EntityKind.SAMPLE);
            if (sampleType.getPermId() == null || sampleType.getPermId().isEmpty())
            {
                throw new UserFailureException(
                        "Mandatory field is missing or empty: " + SAMPLE_TYPE_FIELD);
            }

            // first check that sample type exist.
            SampleTypeFetchOptions fetchTypeOptions = new SampleTypeFetchOptions();
            fetchTypeOptions.withPropertyAssignments().withPropertyType().withVocabulary()
                    .withTerms();
            fetchTypeOptions.withPropertyAssignments();
            SampleType type = (SampleType) objectTypeHelper.getResult().get(sampleType);
            if (type == null)
            {
                throw new UserFailureException("Sample type " + sampleType + " not found.");
            }
            if (type.getPropertyAssignments() == null)
            {
                type.setPropertyAssignments(new ArrayList<>());
            }

            this.propertyTypeSearcher = new PropertyTypeSearcher(type.getPropertyAssignments());

            lineIndex++;
        } catch (Exception e)
        {
            throw new UserFailureException(
                    "sheet: " + (pageIndex + 1) + " line: " + (lineIndex + 1) + " message: " + e.getMessage());
        }

        // and then import samples
        super.importBlock(page, pageIndex, start + 2, end);
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
        Sample sample = new Sample();
        ;
        sample.setType((SampleType) objectTypeHelper.getResult().get(sampleType));
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withType();
        fetchOptions.withSpace();
        fetchOptions.withProject();
        fetchOptions.withExperiment();
        fetchOptions.withChildren();
        fetchOptions.withParents();
        sample.setFetchOptions(fetchOptions);

        String variable = getValueByColumnName(header, values, Attribute.$);
        String code = getValueByColumnName(header, values, Attribute.Code);
        String autoGenerateCode =
                getValueByColumnName(header, values, Attribute.AutoGenerateCode);
        String space = getValueByColumnName(header, values, Attribute.Space);
        String project = getValueByColumnName(header, values, Attribute.Project);
        String identifier =
                getValueByColumnName(header, values, Attribute.Identifier);
        String experiment =
                getValueByColumnName(header, values, Attribute.Experiment);
        String parents = getValueByColumnName(header, values, Attribute.Parents);
        String children =
                getValueByColumnName(header, values, Attribute.Children);
        childrenToResolve.put(code, new ArrayList<>());
        parentsToResolve.put(code, new ArrayList<>());
        collectionsToResolve.put(code, experiment);
        sample.setProject(projectHelper.getProject(project));

        sample.setIdentifier(new SampleIdentifier(identifier));

        if (variable != null && !variable.isEmpty() && !variable.startsWith(VARIABLE_PREFIX))
        {
            throw new UserFailureException("Variables should start with " + VARIABLE_PREFIX);
        }

        if (code != null && !code.isEmpty())
        {
            sample.setCode(code);
        }

        if (space != null && !space.isEmpty())
        {
            sample.setSpace(spaceHelper.getSpace(space));
        }
        if (project != null && !project.isEmpty()) // Projects can only be set in project samples are enabled
        {
            sample.setProject(projectHelper.getProject(project));
        }
        if (experiment != null && !experiment.isEmpty())
        {
            collectionsToResolve.put(code, experiment);
        }

        // Start - Special case - Sample Variables
        if (parents != null && !parents.isEmpty())
        {
            List<SampleIdentifier> parentIds = new ArrayList<>();
            for (String parent : parents.split("\n"))
            {

                parentIds.add(new SampleIdentifier(parent));

            }
            parentsToResolve.put(code, parentIds);
        }
        if (children != null && !children.isEmpty())
        {

            List<SampleIdentifier> childrenIds = new ArrayList<>();
            for (String child : children.split("\n"))
            {

                childrenIds.add(new SampleIdentifier(child));

            }
            childrenToResolve.put(code, childrenIds);
        }
        // End - Special case - Sample Variables

        for (String key : header.keySet())
        {
            if (!attributeValidator.isHeader(key))
            {
                String value = getValueByColumnName(header, values, key);
                PropertyType propertyType = propertyTypeSearcher.findPropertyType(key);
                sample.setProperty(propertyType.getCode(), getPropertyValue(propertyType, value));
            }
        }

        accumulator.put(code, sample);
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

    public void resolveReferences()
    {
        collectionsToResolve.entrySet().forEach(x -> {
            Sample sample =
                    accumulator.get(x.getKey());
            Experiment experiment = collectionHelper.getByUsageCode(x.getValue());
            sample.setExperiment(experiment);

        });
        identifierToSample = accumulator.values().stream().collect(Collectors.toMap(
                x -> "/" + x.getSpace().getCode() + "/" + x.getProject()
                        .getCode() + "/" + x.getCode(), x -> x));

        childrenToResolve.forEach((key, value) -> {
            Sample sample = accumulator.get(key);
            List<Sample> children =
                    value.stream().map(x -> x.getIdentifier()).map(x -> identifierToSample.get(x))
                            .toList();
            sample.setChildren(children);
        });
        parentsToResolve.forEach((key, value) -> {
            Sample sample = accumulator.get(key);
            List<Sample> parents =
                    value.stream().map(x -> x.getIdentifier()).map(x -> identifierToSample.get(x))
                            .toList();
            sample.setParents(parents);
        });

    }

    public Map<ObjectIdentifier, AbstractEntity> getResult()
    {
        return accumulator.values().stream().collect(Collectors.toMap(
                x -> new SampleIdentifier(x.getSpace().getCode(), x.getExperiment().getCode(),
                        x.getCode()), x -> x));
    }

}
