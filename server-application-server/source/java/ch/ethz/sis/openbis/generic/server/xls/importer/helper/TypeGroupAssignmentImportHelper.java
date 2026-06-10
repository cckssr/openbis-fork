package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IPropertyAssignmentsHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyTermCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyTermPermId;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;

import java.util.*;
import java.util.stream.Collectors;

public class TypeGroupAssignmentImportHelper extends BasicImportHelper
{
    private enum Attribute implements IAttribute
    {
        Code("Code", true, true),
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

    private TypeGroup typeGroup;

    private List<TypeGroupAssignment> assignments;

    private final DelayedExecutionDecorator delayedExecutor;

    private final AttributeValidator<Attribute> attributeValidator;

    public TypeGroupAssignmentImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options)
    {
        super(mode, options);
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return ImportTypes.TYPE_GROUP;
    }

    @Override
    protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        boolean isInternalNamespace = ImportUtils.isTrue(internal);

        if(isInternalNamespace && !delayedExecutor.isSystem()) {
            //if exists, skip
            return !isObjectExist(header, values);
        }
        return true;
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        for(TypeGroupAssignment assignment : assignments) {
            if(assignment.getSampleType().getCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        boolean isInternalNamespace = ImportUtils.isTrue(internal);

        TypeGroupAssignmentCreation assignmentCreation = new TypeGroupAssignmentCreation();
        assignmentCreation.setSampleTypeId(new EntityTypePermId(code, EntityKind.SAMPLE));
        assignmentCreation.setTypeGroupId(new TypeGroupId(typeGroup.getCode()));
        if(delayedExecutor.isSystem())
        {
            assignmentCreation.setManagedInternally(isInternalNamespace);
        }
        delayedExecutor.createTypeGroupAssignments(List.of(assignmentCreation), page, line);
    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        //Current assignment was not deleted due to insufficient rights, so we skip update
    }

    @Override
    protected void validateHeader(Map<String, Integer> headers)
    {
        attributeValidator.validateHeaders(Attribute.values(), headers);
    }

    @Override public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        Map<String, Integer> header = parseHeader(page.get(start), false);
        String typeGroupCode = getValueByColumnName(header, page.get(start + 1), "Code");
        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withTypeGroupAssignments().withSampleType();
        typeGroup = delayedExecutor.getTypeGroup(new TypeGroupId(typeGroupCode), fetchOptions);
        if(typeGroup != null) {
            assignments = typeGroup.getTypeGroupAssignments();
        } else {
            assignments = new ArrayList<>();
        }

        List<ITypeGroupAssignmentId> assignmentsToDelete = new ArrayList<>();
        for(TypeGroupAssignment assignment : assignments) {
            TypeGroupAssignmentId id = new TypeGroupAssignmentId(assignment.getSampleType().getPermId(), typeGroup.getId());
            if(delayedExecutor.isSystem() || !assignment.isManagedInternally()) {
                assignmentsToDelete.add(id);
            }
        }
        if(!assignmentsToDelete.isEmpty()) {
            TypeGroupAssignmentDeletionOptions deletionOptions = new TypeGroupAssignmentDeletionOptions();
            delayedExecutor.deleteTypeGroupAssignments(assignmentsToDelete, deletionOptions);
            typeGroup = delayedExecutor.getTypeGroup(new TypeGroupId(typeGroupCode), fetchOptions);
            if(typeGroup != null) {
                assignments = typeGroup.getTypeGroupAssignments();
            } else {
                assignments = new ArrayList<>();
            }
        }

        super.importBlock(page, pageIndex, start + 2, end);
    }


}
