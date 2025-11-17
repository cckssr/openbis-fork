import { FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormFieldDataType, FormSection } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { getFormatedDate } from '@src/js/components/database/new-forms/utils/Utils.ts';

// Helper type for overrides
export type FieldOverrides<T = any> = Partial<Omit<FormField<T>, 'value'>> & { value?: T };

export function getObjectTypeCodeField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-objectTypeCode',
    label: 'Object Type Code',
    value: overrides.value ?? dto.objectTypeCode,
    dataType: FormFieldDataType.CONTROLLED_VOCABULARY,
    required: true,
    readOnly: true,
    isMultiValue: false,
    section: FormSection.SELECT_TYPE,
    column: 'left',
    meta: {},
    ...overrides
  };
}

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
  
