import { FormField, FormFieldDataType, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
import { getFormatedDate } from '@src/js/components/database/new-forms/Utils.ts';

// Helper type for overrides
export type FieldOverrides<T = any> = Partial<Omit<FormField<T>, 'value'>> & { value?: T };

export function getCodeField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  return {
    id: 'code',
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
  return {
    id: 'description',
    label: 'Description',
    value: overrides.value ?? dto.description,
    dataType: FormFieldDataType.MULTILINE_VARCHAR,
    required: false,
    readOnly: false,
    isMultiValue: false,
    section: FormSection.GENERAL,
    column: 'center',
    meta: {},
    ...overrides
  };
}

export function getPermIdField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  return {
    id: 'permId',
    label: 'PermId',
    value: overrides.value ?? dto.permId?.permId,
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
  return {
    id: 'identifier',
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
  return {
    id: 'path',
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
  return {
    id: 'space',
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

export function getRegistratorField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  return {
    id: 'registrator',
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
  return {
    id: 'registrationDate',
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
  return {
    id: 'modifier',
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
  return {
    id: 'modificationDate',
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
  return {
    id: 'entityType',
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
} 