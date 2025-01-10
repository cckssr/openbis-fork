package ch.eth.sis.rocrate.parser.helper;

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
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.SampleImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.generic.server.xls.importer.helper.SampleImportHelper.SAMPLE_TYPE_FIELD;
import static ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher.VARIABLE_PREFIX;
import static ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher.getPropertyValue;

public class ObjectHelper extends BasicImportHelper
{

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

    private final AttributeValidator<SampleImportHelper.Attribute> attributeValidator;

    PropertyTypeSearcher propertyTypeSearcher;

    CollectionHelper collectionHelper;

    public ObjectHelper(ImportModes mode,
            ImportOptions options, SpaceHelper spaceHelper,
            ProjectHelper projectHelper,
            CollectionTypeHelper collectionTypeHelper, ObjectTypeHelper objectTypeHelper,
            CollectionHelper collectionHelper)
    {
        super(mode, options);
        this.attributeValidator = new AttributeValidator<>(SampleImportHelper.Attribute.class);
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
    protected ImportTypes getTypeName()
    {
        return null;
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

        String variable = getValueByColumnName(header, values, SampleImportHelper.Attribute.$);
        String code = getValueByColumnName(header, values, SampleImportHelper.Attribute.Code);
        String autoGenerateCode =
                getValueByColumnName(header, values, SampleImportHelper.Attribute.AutoGenerateCode);
        String space = getValueByColumnName(header, values, SampleImportHelper.Attribute.Space);
        String project = getValueByColumnName(header, values, SampleImportHelper.Attribute.Project);
        String identifier =
                getValueByColumnName(header, values, SampleImportHelper.Attribute.Identifier);
        String experiment =
                getValueByColumnName(header, values, SampleImportHelper.Attribute.Experiment);
        String parents = getValueByColumnName(header, values, SampleImportHelper.Attribute.Parents);
        String children =
                getValueByColumnName(header, values, SampleImportHelper.Attribute.Children);
        childrenToResolve.put(code, new ArrayList<>());
        parentsToResolve.put(code, new ArrayList<>());
        collectionsToResolve.put(code, experiment);

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
        if (project != null && !project.isEmpty() && options.getAllowProjectSamples()) // Projects can only be set in project samples are enabled
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
                x -> x.getSpace().getCode() + "/" + x.getProject() + "/" + x.getCode(), x -> x));

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
