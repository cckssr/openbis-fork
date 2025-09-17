import { FormField, FormSection } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

// Helper type for field overrides
export type FieldOverrides<T = any> = Partial<Omit<FormField<T>, 'value'>> & { value?: T };

/**
 * Format date for display
 */
export function formatDate(date: Date | string | number): string {
  if (!date) return '';
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
}

/**
 * Get code field for any entity
 */
export function getCodeField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-code',
    name: 'code',
    label: 'Code',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? dto.code ?? '',
    required: true,
    readOnly: overrides.readOnly ?? true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get description field for any entity
 */
export function getDescriptionField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-description',
    name: 'description',
    label: 'Description',
    type: 'textarea',
    dataType: 'MULTILINE_VARCHAR',
    value: overrides.value ?? dto.description ?? '',
    required: false,
    readOnly: overrides.readOnly ?? false,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get permId field for any entity
 */
export function getPermIdField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-permId',
    name: 'permId',
    label: 'PermId',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? permId,
    required: true,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get identifier field for any entity
 */
export function getIdentifierField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-identifier',
    name: 'identifier',
    label: 'Identifier',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? dto.identifier?.identifier ?? '',
    required: true,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get path field for any entity
 */
export function getPathField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-path',
    name: 'path',
    label: 'Path',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? dto.identifier?.identifier ?? '',
    required: true,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get space field for project entities
 */
export function getSpaceField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-space',
    name: 'space',
    label: 'Space',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? dto.space?.code ?? '',
    required: true,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get registrator field for any entity
 */
export function getRegistratorField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-registrator',
    name: 'registrator',
    label: 'Registrator',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? dto.registrator?.userId ?? '',
    required: false,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get registration date field for any entity
 */
export function getRegistrationDateField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-registrationDate',
    name: 'registrationDate',
    label: 'Registration Date',
    type: 'date',
    dataType: 'TIMESTAMP',
    value: overrides.value ?? (dto.registrationDate ? formatDate(dto.registrationDate) : ''),
    required: false,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get modifier field for any entity
 */
export function getModifierField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-modifier',
    name: 'modifier',
    label: 'Modifier',
    type: 'text',
    dataType: 'VARCHAR',
    value: overrides.value ?? dto.modifier ?? '',
    required: false,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}

/**
 * Get modification date field for any entity
 */
export function getModificationDateField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId?.permId || dto.id || 'unknown';
  return {
    id: permId + '-modificationDate',
    name: 'modificationDate',
    label: 'Modification Date',
    type: 'date',
    dataType: 'TIMESTAMP',
    value: overrides.value ?? (dto.modificationDate ? formatDate(dto.modificationDate) : ''),
    required: false,
    readOnly: true,
    validation: [],
    visibility: [],
    options: [],
    meta: {},
    ...overrides
  };
}
