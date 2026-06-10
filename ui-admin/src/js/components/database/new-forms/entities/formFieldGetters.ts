import {FormField} from '@src/js/components/database/new-forms/types/formITypes.ts';
import {
  FormFieldDataType,
  FormSection,
  Widget
} from '@src/js/components/database/new-forms/types/formEnums.ts';
import {getFormattedTimestamp} from '@src/js/components/database/new-forms/utils/dateUtil.ts';

// Helper type for overrides
export type FieldOverrides<T = any> = Partial<Omit<FormField<T>, 'value'>> & { value?: T };

export function getCodeField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  const readOnly = overrides.readOnly !== undefined ? overrides.readOnly : true;
  const value = overrides.value ?? dto.code;
  const field: FormField<string> = {
    id: permId + '-code',
    label: 'Code',
    value,
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly,
    isMultiValue: false,
    section: FormSection.IDENTIFICATION_INFO,
    column: 'left',
    meta: {},
    ...overrides
  };
  
  // Only set initialValue for non-readonly fields
  if (!readOnly) {
    field.initialValue = overrides.initialValue !== undefined ? overrides.initialValue : value;
  }
  
  return field;
}

export function getDescriptionField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  const readOnly = overrides.readOnly !== undefined ? overrides.readOnly : false;
  const value = overrides.value ?? dto.description;
  const field: FormField<string> = {
    id: permId + '-description',
    label: 'Description',
    value,
    dataType: FormFieldDataType.WORD_PROCESSOR,
    required: false,
    readOnly,
    isMultiValue: false,
    section: FormSection.GENERAL,
    column: 'center',
    meta: {
      mode: 'inline'
    },
    ...overrides
  };
  
  // Only set initialValue for non-readonly fields
  if (!readOnly) {
    field.initialValue = overrides.initialValue !== undefined ? overrides.initialValue : value;
  }
  
  return field;
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

export function getCollectionField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-collection',
    label: 'Collection',
    value: overrides.value ?? dto.experiment?.identifier?.identifier,
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

export function getObjectField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-object',
    label: 'Object',
    value: overrides.value ?? dto.sample?.identifier?.identifier,
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
    value: overrides.value ?? (dto.registrationDate ? getFormattedTimestamp(dto.registrationDate) : ''),
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
    value: overrides.value ?? dto.modifier?.userId,
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
    value: overrides.value ?? (dto.modificationDate ? getFormattedTimestamp(dto.modificationDate) : ''),
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

/**
 * Gets a property value from DTO based on dataType and multiValue flag
 * Automatically calls the appropriate DTO getter method
 * 
 * @param dto - The DTO object with property getter methods
 * @param propertyCode - The property code/name
 * @param dataType - The FormFieldDataType
 * @param isMultiValue - Whether this is a multi-value property
 * @returns The property value
 */
export function getPropertyValue(
  dto: any,
  propertyCode: string,
  dataType: FormFieldDataType,
  isMultiValue: boolean = false
): any {
  if (!dto || !propertyCode) {
    return null;
  }

  // Handle multi-value properties
  if (isMultiValue) {
    switch (dataType) {
      case FormFieldDataType.INTEGER:
        return dto.getMultiValueIntegerProperty?.(propertyCode) || null;
      case FormFieldDataType.REAL:
        return dto.getMultiValueRealProperty?.(propertyCode) || null;
      case FormFieldDataType.VARCHAR:
      case FormFieldDataType.MULTILINE_VARCHAR:
        return dto.getMultiValueStringProperty?.(propertyCode) || null;
      case FormFieldDataType.TIMESTAMP:
        return dto.getMultiValueTimestampProperty?.(propertyCode) || null;
      case FormFieldDataType.BOOLEAN:
        return dto.getMultiValueBooleanProperty?.(propertyCode) || null;
      case FormFieldDataType.HYPERLINK:
        return dto.getMultiValueHyperlinkProperty?.(propertyCode) || null;
      case FormFieldDataType.CONTROLLEDVOCABULARY:
        return dto.getMultiValueControlledVocabularyProperty?.(propertyCode) || null;
      case FormFieldDataType.SAMPLE:
        return dto.getMultiValueSampleProperty?.(propertyCode) || null;
      case FormFieldDataType.SPREADSHEET:
        // Multi-value spreadsheet uses JSON array
        return dto.getMultiValueJsonProperty?.(propertyCode) || null;
      case FormFieldDataType.JSON:
        return dto.getMultiValueJsonProperty?.(propertyCode) || null;
      case FormFieldDataType.XML:
        return dto.getMultiValueXmlProperty?.(propertyCode) || null;
      case FormFieldDataType.ARRAY_INTEGER:
        return dto.getMultiValueIntegerArrayProperty?.(propertyCode) || null;
      case FormFieldDataType.ARRAY_REAL:
        return dto.getMultiValueRealArrayProperty?.(propertyCode) || null;
      case FormFieldDataType.ARRAY_STRING:
        return dto.getMultiValueStringArrayProperty?.(propertyCode) || null;
      case FormFieldDataType.ARRAY_TIMESTAMP:
        return dto.getMultiValueTimestampArrayProperty?.(propertyCode) || null;
      default:
        // Fallback to generic getProperty for unknown types
        return dto.getProperty?.(propertyCode) || null;
    }
  }

  // Handle single-value properties
  switch (dataType) {
    case FormFieldDataType.INTEGER:
      return dto.getIntegerProperty?.(propertyCode) || null;
    case FormFieldDataType.REAL:
      return dto.getRealProperty?.(propertyCode) || null;
    case FormFieldDataType.VARCHAR:
    case FormFieldDataType.MULTILINE_VARCHAR:
      return dto.getStringProperty?.(propertyCode) || null;
    case FormFieldDataType.TIMESTAMP:
      return dto.getTimestampProperty?.(propertyCode) || null;
    case FormFieldDataType.BOOLEAN:
      return dto.getBooleanProperty?.(propertyCode) || null;
    case FormFieldDataType.CONTROLLEDVOCABULARY:
      return dto.getControlledVocabularyProperty?.(propertyCode) || null;
    case FormFieldDataType.SAMPLE:
      return dto.getSampleProperty?.(propertyCode) || null;
    case FormFieldDataType.HYPERLINK:
      return dto.getHyperlinkProperty?.(propertyCode) || null;
    case FormFieldDataType.SPREADSHEET:
      return dto.getSpreadsheetProperty?.(propertyCode) || null;
    case FormFieldDataType.WORD_PROCESSOR:
    case FormFieldDataType.WORD_PROCESSOR_PAGE:
    case FormFieldDataType.WORD_PROCESSOR_CLASSIC:
    case FormFieldDataType.MONOSPACE_FONT:
      return dto.getRichTextProperty?.(propertyCode) || null;
    case FormFieldDataType.JSON:
      return dto.getJsonProperty?.(propertyCode) || null;
    case FormFieldDataType.XML:
      return dto.getXmlProperty?.(propertyCode) || null;
    case FormFieldDataType.ARRAY_INTEGER:
      return dto.getIntegerArrayProperty?.(propertyCode) || null;
    case FormFieldDataType.ARRAY_REAL:
      return dto.getRealArrayProperty?.(propertyCode) || null;
    case FormFieldDataType.ARRAY_STRING:
      return dto.getStringArrayProperty?.(propertyCode) || null;
    case FormFieldDataType.ARRAY_TIMESTAMP:
      return dto.getTimestampArrayProperty?.(propertyCode) || null;
    default:
      // Fallback to generic getProperty for unknown types
      return dto.getProperty?.(propertyCode) || null;
  }
}

/**
 * Sets a property value on DTO based on dataType and multiValue flag
 * Automatically calls the appropriate DTO setter method
 * 
 * @param dto - The DTO object with property setter methods
 * @param propertyCode - The property code/name
 * @param value - The value to set
 * @param dataType - The FormFieldDataType
 * @param isMultiValue - Whether this is a multi-value property
 */
export function setPropertyValue(
  dto: any,
  propertyCode: string,
  value: any,
  dataType: FormFieldDataType,
  isMultiValue: boolean = false
): void {
  if (!dto || !propertyCode) {
    return;
  }

  // Handle multi-value properties
  if (isMultiValue) {
    switch (dataType) {
      case FormFieldDataType.INTEGER:
        dto.setMultiValueIntegerProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.REAL:
        dto.setMultiValueRealProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.VARCHAR:
      case FormFieldDataType.MULTILINE_VARCHAR:
        dto.setMultiValueStringProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.TIMESTAMP:
        dto.setMultiValueTimestampProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.BOOLEAN:
        dto.setMultiValueBooleanProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.HYPERLINK:
        dto.setMultiValueHyperlinkProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.CONTROLLEDVOCABULARY:
        dto.setMultiValueControlledVocabularyProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.SAMPLE:
        dto.setMultiValueSampleProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.SPREADSHEET:
        // Multi-value spreadsheet uses JSON array
        dto.setMultiValueJsonProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.JSON:
        dto.setMultiValueJsonProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.XML:
        dto.setMultiValueXmlProperty?.(propertyCode, value);
        return;
      case FormFieldDataType.ARRAY_INTEGER:
        dto.setMultiValueIntegerArrayProperty?.(propertyCode, normalizeMultiValueArrayPropertyValues(value, dataType));
        return;
      case FormFieldDataType.ARRAY_REAL:
        dto.setMultiValueRealArrayProperty?.(propertyCode, normalizeMultiValueArrayPropertyValues(value, dataType));
        return;
      case FormFieldDataType.ARRAY_STRING:
        dto.setMultiValueStringArrayProperty?.(propertyCode, normalizeMultiValueArrayPropertyValues(value, dataType));
        return;
      case FormFieldDataType.ARRAY_TIMESTAMP:
        dto.setMultiValueTimestampArrayProperty?.(propertyCode, normalizeMultiValueArrayPropertyValues(value, dataType));
        return;
      default:
        // Fallback to generic setProperty for unknown types
        dto.setProperty?.(propertyCode, value);
        return;
    }
  }

  // Handle single-value properties
  switch (dataType) {
    case FormFieldDataType.INTEGER:
      dto.setIntegerProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.REAL:
      dto.setRealProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.VARCHAR:
    case FormFieldDataType.MULTILINE_VARCHAR:
      dto.setStringProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.TIMESTAMP:
      dto.setTimestampProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.BOOLEAN:
      dto.setBooleanProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.CONTROLLEDVOCABULARY:
      dto.setControlledVocabularyProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.SAMPLE:
      dto.setSampleProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.HYPERLINK:
      dto.setHyperlinkProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.SPREADSHEET:
      dto.setSpreadsheetProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.WORD_PROCESSOR:
    case FormFieldDataType.WORD_PROCESSOR_PAGE:
    case FormFieldDataType.WORD_PROCESSOR_CLASSIC:
    case FormFieldDataType.MONOSPACE_FONT:
      dto.setRichTextProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.JSON:
      dto.setJsonProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.XML:
      dto.setXmlProperty?.(propertyCode, value);
      return;
    case FormFieldDataType.ARRAY_INTEGER:
      dto.setIntegerArrayProperty?.(propertyCode, normalizeArrayPropertyValue(value, dataType));
      return;
    case FormFieldDataType.ARRAY_REAL:
      dto.setRealArrayProperty?.(propertyCode, normalizeArrayPropertyValue(value, dataType));
      return;
    case FormFieldDataType.ARRAY_STRING:
      dto.setStringArrayProperty?.(propertyCode, normalizeArrayPropertyValue(value, dataType));
      return;
    case FormFieldDataType.ARRAY_TIMESTAMP:
      dto.setTimestampArrayProperty?.(propertyCode, normalizeArrayPropertyValue(value, dataType));
      return;
    default:
      // Fallback to generic setProperty for unknown types
      dto.setProperty?.(propertyCode, value);
      return;
  }
}

function normalizeMultiValueArrayPropertyValues(values: any[], dataType: FormFieldDataType): any[][] {
  return values.map(value => normalizeArrayPropertyValue(value, dataType));
}

function normalizeArrayPropertyValue(value: any, dataType: FormFieldDataType): any[] {
  let parsedValue = value;

  if (typeof parsedValue === 'string') {
    const trimmedValue = parsedValue.trim();
    parsedValue = trimmedValue === '' ? [] : JSON.parse(trimmedValue);
  }

  if (!Array.isArray(parsedValue)) {
    throw new Error(`Property value for ${dataType} must be an array.`);
  }

  switch (dataType) {
    case FormFieldDataType.ARRAY_INTEGER:
      return parsedValue.map(item => Number.parseInt(item, 10));
    case FormFieldDataType.ARRAY_REAL:
      return parsedValue.map(item => Number(item));
    default:
      return parsedValue;
  }
}

/**
 * Maps DTO dataType string to FormFieldDataType enum
 */
function mapDataTypeToFormFieldDataType(dtoDataType: string, customWidget?: string): FormFieldDataType {
  // Check for custom widget first
  if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === Widget.WORD_PROCESSOR) {
    return FormFieldDataType.WORD_PROCESSOR;
  } else if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === Widget.WORD_PROCESSOR_PAGE) {
    return FormFieldDataType.WORD_PROCESSOR_PAGE;
  } else if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === Widget.WORD_PROCESSOR_CLASSIC) {
    return FormFieldDataType.WORD_PROCESSOR_CLASSIC;
  } else if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === Widget.MONOSPACE_FONT) {
    return FormFieldDataType.MONOSPACE_FONT;
  } else if (dtoDataType === FormFieldDataType.MULTILINE_VARCHAR && customWidget === Widget.SPREADSHEET) {
    return FormFieldDataType.SPREADSHEET;
  } else {
    return dtoDataType as FormFieldDataType;  
  }
}

/**
 * Maps a property assignment to a FormField object
 * 
 * @param assignment - The property assignment from DTO
 * @param dto - The DTO object containing property values
 * @param permId - The permanent ID for the entity
 * @param overrides - Optional overrides for the field
 * @returns FormField object
 */
function mapAssignmentToFormField(
  assignment: any,
  dto: any,
  permId: string,
  overrides: Record<string, FieldOverrides>
): FormField {
  const propertyType = assignment.propertyType;
  const propertyCode = propertyType.code;
  const fieldId = `${permId}-${propertyCode}`;
  const fieldOverrides = overrides[propertyCode] || {};

  // Determine dataType - check custom_widget first, then dataType
  const customWidget = propertyType.metaData?.custom_widget;
  const dataType = mapDataTypeToFormFieldDataType(propertyType.dataType, customWidget);

  const section = fieldOverrides.section ?? assignment.section ?? FormSection.UNKNOWN;
  const isMultiValue = propertyType.multiValue || false;

  // Extract value using the appropriate getter method based on dataType
  const propertyValue = getPropertyValue(dto, propertyCode, dataType, isMultiValue) ?? '';

  const column = determineFieldColumn(dataType, section, assignment.ordinal);
  const meta = buildFieldMeta(propertyType.metaData, dataType, propertyType);

  const readOnly = fieldOverrides.readOnly !== undefined 
    ? fieldOverrides.readOnly 
    : !(assignment.showInEditView ?? true);
  const value = fieldOverrides.value !== undefined ? fieldOverrides.value : propertyValue;
  
  const options = propertyType.vocabulary && propertyType.vocabulary.terms ? propertyType.vocabulary.terms.map((term: any) => ({ label: term.label, value: term.code })) : [];

  const field: FormField = {
    id: fieldId,
    name: propertyCode,
    label: propertyType.label || propertyCode,
    value,
    dataType: fieldOverrides.dataType || dataType,
    required: fieldOverrides.required !== undefined 
      ? fieldOverrides.required 
      : (assignment.mandatory || false),
    readOnly,
    isMultiValue: fieldOverrides.isMultiValue !== undefined 
      ? fieldOverrides.isMultiValue 
      : (propertyType.multiValue || false),
    section: section,
    column: fieldOverrides.column || column,
    meta: { ...meta, ...(fieldOverrides.meta || {}) },
    options,
    ...fieldOverrides
  };

  // Only set initialValue for non-readonly fields
  if (!readOnly) {
    field.initialValue = fieldOverrides.initialValue !== undefined 
      ? fieldOverrides.initialValue 
      : value;
  }

  return field;
}

/**
 * Determines the column placement for a field based on dataType and section
 * 
 * @param dataType - The FormFieldDataType
 * @param section - The FormSection
 * @param ordinal - The ordinal position of the assignment
 * @returns Column placement ('left', 'right', or 'center')
 */
function determineFieldColumn(
  dataType: FormFieldDataType,
  section: FormSection,
  ordinal: number
): 'left' | 'right' | 'center' {
  // Center column for word processor and spreadsheet types
  if ([
    FormFieldDataType.WORD_PROCESSOR,
    FormFieldDataType.WORD_PROCESSOR_PAGE,
    FormFieldDataType.WORD_PROCESSOR_CLASSIC,
    FormFieldDataType.MONOSPACE_FONT,
    FormFieldDataType.MULTILINE_VARCHAR,
    FormFieldDataType.SPREADSHEET
  ].includes(dataType)) {
    return 'center';
  }
  
  // Right column for identification info fields with ordinal > 5
  if (section === FormSection.IDENTIFICATION_INFO && ordinal > 5) {
    return 'right';
  }
  
  // Default to left
  return 'left';
}

/**
 * Builds the meta object for a field based on propertyType metadata and dataType
 * 
 * @param propertyMetaData - Metadata from propertyType
 * @param dataType - The FormFieldDataType
 * @returns Meta object
 */
function buildFieldMeta(propertyMetaData: any, dataType: FormFieldDataType, propertyType?: any): any {
  const meta: any = { ...(propertyMetaData || {}) };

  // Add mode for word processor types
  if (dataType === FormFieldDataType.WORD_PROCESSOR) {
    meta.mode = 'inline';
  } else if (dataType === FormFieldDataType.WORD_PROCESSOR_PAGE) {
    meta.mode = 'document';
  } else if (dataType === FormFieldDataType.WORD_PROCESSOR_CLASSIC) {
    meta.mode = 'classic';
  }

  if (dataType === FormFieldDataType.SAMPLE && propertyType?.sampleType?.code) {
    meta.sampleTypeCode = propertyType.sampleType.code;
  }

  return meta;
}

/**
 * Sorts form fields by section and ordinal
 * 
 * @param fields - Array of FormField objects to sort
 * @param propertyAssignments - Array of property assignments from DTO (for ordinal lookup)
 * @returns Sorted array of FormField objects
 */
function sortFormFieldsBySectionAndOrdinal(
  fields: FormField[],
  propertyAssignments: any[]
): FormField[] {
  const sectionOrder = [
    FormSection.SELECT_TYPE,
    FormSection.IDENTIFICATION_INFO,
    FormSection.GENERAL,
    FormSection.OVERVIEW,
    FormSection.METADATA,
    FormSection.UNKNOWN
  ];

  return fields.sort((a: FormField, b: FormField) => {
    // Sort by section first
    if (a.section !== b.section) {
      const sectionIndexA = sectionOrder.indexOf(a.section);
      const sectionIndexB = sectionOrder.indexOf(b.section);
      // If section not found in order, treat as -1 (put at end)
      const indexA = sectionIndexA === -1 ? sectionOrder.length : sectionIndexA;
      const indexB = sectionIndexB === -1 ? sectionOrder.length : sectionIndexB;
      return indexA - indexB;
    }
    
    // Then sort by ordinal
    const assignmentA = propertyAssignments.find((pa: any) => pa.propertyType.code === a.name);
    const assignmentB = propertyAssignments.find((pa: any) => pa.propertyType.code === b.name);
    const ordinalA = assignmentA?.ordinal || 0;
    const ordinalB = assignmentB?.ordinal || 0;
    
    return ordinalA - ordinalB;
  });
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
  const permId = dto.permId?.permId || 'unknown';
  if (dto.propertyAssignments && Array.isArray(dto.propertyAssignments)) {
    return dto.propertyAssignments.map((assignment: any) => {
      overrides[assignment.propertyType.code] = { readOnly: false };
      return mapAssignmentToFormField(assignment, dto, permId, overrides) }
    );
  }
  if (!dto?.type?.propertyAssignments || !Array.isArray(dto.type.propertyAssignments)) {
    return [];
  }
  
  const propertyAssignments = dto.type.propertyAssignments.filter(
    (assignment: any) => assignment?.propertyType
  );

  const fields = propertyAssignments.map((assignment: any) =>
    mapAssignmentToFormField(assignment, dto, permId, overrides)
  );

  return sortFormFieldsBySectionAndOrdinal(fields, propertyAssignments);
}
  
