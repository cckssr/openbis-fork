/**
 * Represents the mode in which the form is currently operating.
 */
export enum FormMode {
  VIEW = 'view',
  NEW = 'new',
  EDIT = 'edit',
}

export enum EntityKind {
  SPACE = 'SPACE',
  PROJECT = 'PROJECT',
  SAMPLE = 'SAMPLE'
}

/**
 * Defines the data type for a form field, mapping to openBIS property types
 * and custom UI widget types.
 */
export enum FormFieldDataType {
  // openBIS Data Types
  VARCHAR = 'VARCHAR',
  MULTILINE_VARCHAR = 'MULTILINE_VARCHAR',
  INTEGER = 'INTEGER',
  REAL = 'REAL',
  TIMESTAMP = 'TIMESTAMP',
  BOOLEAN = 'BOOLEAN',
  CONTROLLED_VOCABULARY = 'CONTROLLED_VOCABULARY',
  HYPERLINK = 'HYPERLINK',
  SAMPLE = 'SAMPLE',
  // Custom UI-specific Types
  WORD_PROCESSOR = 'WORD_PROCESSOR',
  SPREADSHEET = 'SPREADSHEET',
}

/**
 * Metadata for a form field, allowing for extensions like help texts,
 * freezing logic, or vocabulary options.
 */
export interface FormFieldMeta {
  isFrozen?: boolean;
  vocabularyOptions?: { code: string; label: string }[];
  [key: string]: any; // For future extensions
}


export enum FormSection {
  IDENTIFICATION_INFO = 'Identification Info',
  GENERAL = 'General',
  OVERVIEW = 'Overview'
}

/**
 * Represents a single field within a form.
 */
export interface FormField<T = any> {
  id: string; // Corresponds to PropertyType code
  label: string;
  value: T;
  dataType: FormFieldDataType;
  isMandatory: boolean;
  isMultiValue: boolean;
  isEditable: boolean;
  section: FormSection;
  meta: FormFieldMeta;
}

export function findFormFieldById(fields: FormField[], fieldId: string): FormField | undefined {
  return fields.find(field => field.id === fieldId);
}

/**
 * The unified Form DTO that represents any entity to be displayed or edited.
 */
export interface Form {
  entityPermId: string;
  entityKind: string;
  entityType: string;
  title: string;
  fields: FormField[];
  version: number;
  meta: {
    [key: string]: any; // For entity-specific metadata
  };
}