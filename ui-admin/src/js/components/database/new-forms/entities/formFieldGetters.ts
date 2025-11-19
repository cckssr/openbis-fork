import { FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormFieldDataType, FormSection } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { getFormatedDate } from '@src/js/components/database/new-forms/utils/Utils.ts';

// Helper type for overrides
export type FieldOverrides<T = any> = Partial<Omit<FormField<T>, 'value'>> & { value?: T };

export function getCodeField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-code',
    label: 'Code',
    value: overrides.value ?? dto.code,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
}

export function getDescriptionField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-description',
    label: 'Description',
    value: overrides.value ?? dto.description,
    dataType: FormFieldDataType.WORD_PROCESSOR,
    required: false,
    readOnly: false,
    isMultiValue: false,
    section: FormSection.GENERAL,
    column: 'center',
    meta: {
      mode: 'inline'
    },
    ...overrides
  };
}

export function getPermIdField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-permId',
    label: 'PermId',
    value: overrides.value ?? permId,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
}

export function getIdentifierField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-identifier',
    label: 'Identifier',
    value: overrides.value ?? dto.identifier?.identifier,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
}

export function getPathField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-path',
    label: 'Path',
    value: overrides.value ?? dto.identifier?.identifier,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
}

export function getSpaceField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-space',
    label: 'Space',
    value: overrides.value ?? dto.space?.code,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
}

export function getProjectField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-project',
    label: 'Project',
    value: overrides.value ?? dto.project?.code,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
}

export function getRegistratorField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-registrator',
    label: 'Registrator',
    value: overrides.value ?? dto.registrator?.userId,
    dataType: FormFieldDataType.VARCHAR,
    required: false,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'right',
    meta: {},
    ...overrides
  };
}

export function getRegistrationDateField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-registrationDate',
    label: 'Registration Date',
    value: overrides.value ?? (dto.registrationDate ? getFormatedDate(new Date(dto.registrationDate)) : ''),
    dataType: FormFieldDataType.TIMESTAMP,
    required: false,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'right',
    meta: {},
    ...overrides
  };
}

export function getModifierField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-modifier',
    label: 'Modifier',
    value: overrides.value ?? dto.modifier,
    dataType: FormFieldDataType.VARCHAR,
    required: false,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'right',
    meta: {},
    ...overrides
  };
}

export function getModificationDateField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-modificationDate',
    label: 'Modification Date',
    value: overrides.value ?? (dto.modificationDate ? getFormatedDate(new Date(dto.modificationDate)) : ''),
    dataType: FormFieldDataType.TIMESTAMP,
    required: false,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'right',
    meta: {},
    ...overrides
  };
} 

export function getTypeField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-entityType',
    label: 'Type',
    value: overrides.value ?? dto.type.code,
    dataType: FormFieldDataType.VARCHAR,
    required: false,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
};

export function getShowOnProjectOverviewField(dto: any, overrides: FieldOverrides = {}): FormField<boolean> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-showOnProjectOverview',
    label: 'Show On Project Overview',
    value: overrides.value ?? dto.showOnProjectOverview,
    dataType: FormFieldDataType.BOOLEAN,
    required: false,
    readOnly: false,
    isMultiValue: false,
    section: FormSection.GENERAL,
    column: 'left',
    meta: {},
    ...overrides
  };
};

export function getDocumentField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-document',
    label: 'Document',
    value: overrides.value ?? dto.properties.DOCUMENT,
    dataType: FormFieldDataType.WORD_PROCESSOR,
    required: false,
    readOnly: false,
    isMultiValue: false,
    section: FormSection.GENERAL,
    column: 'center',
    meta: {},
    ...overrides
  };
}

/**
 * Maps DTO dataType string to FormFieldDataType enum
 */
function mapDataTypeToFormFieldDataType(dtoDataType: string, customWidget?: string): FormFieldDataType {
  // Check for custom widget first
  if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === 'Word Processor') {
    return FormFieldDataType.WORD_PROCESSOR;
  } else if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === 'Word Processor Page') {
    return FormFieldDataType.WORD_PROCESSOR_PAGE;
  } else {
    return dtoDataType as FormFieldDataType;  
  }
}

/**
 * Generates form fields dynamically from propertyAssignments in the DTO.
 * This is a generic function that can be used by any entity type.
 * 
 * @param dto - The DTO object containing type.propertyAssignments and properties
 * @param overrides - Optional overrides for individual fields (keyed by property code)
 * @returns Array of FormField objects sorted by ordinal
 */
export function getPropertyFieldsFromAssignments(
  dto: any,
  overrides: Record<string, FieldOverrides> = {}
): FormField[] {
  if (!dto?.type?.propertyAssignments || !Array.isArray(dto.type.propertyAssignments)) {
    return [];
  }

  const permId = dto.permId?.permId || 'unknown';
  const properties = dto.properties || {};

  const fields = dto.type.propertyAssignments
    .filter((assignment: any) => assignment?.propertyType) // Filter out invalid assignments
    .map((assignment: any) => {
      const propertyType = assignment.propertyType;
      const propertyCode = propertyType.code;
      const fieldId = `${permId}-${propertyCode}`;
      const fieldOverrides = overrides[propertyCode] || {};

      // Determine dataType - check custom_widget first, then dataType
      const customWidget = propertyType.metaData?.custom_widget;
      const dataType = mapDataTypeToFormFieldDataType(propertyType.dataType, customWidget);

      // Determine section: use override if provided, otherwise assignment section, otherwise UNKNOWN
      const section = fieldOverrides.section ?? assignment.section ?? FormSection.UNKNOWN;

      // Extract value from properties
      const propertyValue = properties[propertyCode] ?? '';

      // Determine column based on section (default to 'left' for GENERAL, 'center' for word processor)
      let column: 'left' | 'right' | 'center' = 'left';
      if (dataType === FormFieldDataType.WORD_PROCESSOR || dataType === FormFieldDataType.WORD_PROCESSOR_PAGE || dataType === FormFieldDataType.MULTILINE_VARCHAR) {
        column = 'center';
      } else if (section === FormSection.IDENTIFICATION_INFO && assignment.ordinal > 5) {
        column = 'right';
      }

      // Build meta object from propertyType.metaData
      const meta: any = { ...(propertyType.metaData || {}) };
      if (customWidget) {
        meta.custom_widget = customWidget;
      }
      if (dataType === FormFieldDataType.WORD_PROCESSOR) {
        meta.mode = 'classic';
      }

      const field: FormField = {
        id: fieldId,
        name: propertyCode,
        label: propertyType.label || propertyCode,
        value: fieldOverrides.value !== undefined ? fieldOverrides.value : propertyValue,
        dataType: fieldOverrides.dataType || dataType,
        required: fieldOverrides.required !== undefined ? fieldOverrides.required : (assignment.mandatory || false),
        readOnly: fieldOverrides.readOnly !== undefined ? fieldOverrides.readOnly : !(assignment.showInEditView ?? true),
        isMultiValue: fieldOverrides.isMultiValue !== undefined ? fieldOverrides.isMultiValue : (propertyType.multiValue || false),
        section: section,
        column: fieldOverrides.column || column,
        meta: { ...meta, ...(fieldOverrides.meta || {}) },
        ...fieldOverrides
      };

      return field;
    })
    .sort((a: FormField, b: FormField) => {
      // Sort by section first, then by ordinal if available
      const assignmentA = dto.type.propertyAssignments.find((pa: any) => pa.propertyType.code === a.name);
      const assignmentB = dto.type.propertyAssignments.find((pa: any) => pa.propertyType.code === b.name);
      const ordinalA = assignmentA?.ordinal || 0;
      const ordinalB = assignmentB?.ordinal || 0;
      
      if (a.section !== b.section) {
        // Sort sections in a logical order
        const sectionOrder = [
          FormSection.SELECT_TYPE,
          FormSection.IDENTIFICATION_INFO,
          FormSection.GENERAL,
          FormSection.OVERVIEW,
          FormSection.METADATA,
          FormSection.UNKNOWN
        ];
        const sectionIndexA = sectionOrder.indexOf(a.section);
        const sectionIndexB = sectionOrder.indexOf(b.section);
        // If section not found in order, treat as -1 (put at end)
        const indexA = sectionIndexA === -1 ? sectionOrder.length : sectionIndexA;
        const indexB = sectionIndexB === -1 ? sectionOrder.length : sectionIndexB;
        return indexA - indexB;
      }
      
      return ordinalA - ordinalB;
    });

  return fields;
}
  
