/**
 * Defines the possible modes for a form.
 * - 'view': Read-only mode.
 * - 'edit': Modifying an existing entity.
 * - 'create': Creating a new entity.
 */
export enum FormMode {
  VIEW = 'view',
  CREATE = 'create',
  EDIT = 'edit',
}

export enum EntityKind {
  SPACE = 'SPACE',
  PROJECT = 'PROJECT',
  EXPERIMENT = 'EXPERIMENT',
  NEW_PROJECT = 'NEWPROJECT',
  OBJECT = 'OBJECT',
  SAMPLE = 'SAMPLE',
  COLLECTION = 'COLLECTION',
  DATASET = 'DATASET'
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

export enum Widget {
  RICH_TEXT = 'RichText',
  SPREADSHEET = 'Spreadsheet'
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
  id: string;
  name?: string; 
  label: string;
  value: T;
  initialValue?: any; 
  dataType: FormFieldDataType;
  required: boolean;
  readOnly: boolean;
  isMultiValue: boolean;
  section: FormSection;
  column?: 'left' | 'right' | 'center';
  meta: FormFieldMeta;
  options?: { label: string; value: string }[]; // For 'select' or 'multiselect' fields
  validation?: (value: any, form: Form) => string | null; // Validation function
}

export function findFormFieldById(fields: FormField[], fieldId: string): FormField | undefined {
  return fields.find(field => field.id === fieldId);
}

/**
 * The unified Form DTO that represents any entity to be displayed or edited.
 */
export interface Form {
  entityPermId: string; // The unique identifier of the entity (permId, code, etc.)
  entityKind: string; // e.g., 'Space', 'Project', 'Sample'
  entityType: string;
  title: string;
  fields: FormField[];
  version: number;
  mode?: FormMode;
  isDirty: boolean; // True if any field has been modified
  isValid: boolean; // True if all fields are valid
  meta: {
    [key: string]: any; // For entity-specific metadata
  };
  actions?: FormAction[];
}

/**
 * Defines the structure for an action that can be performed from the form's toolbar.
 */
export interface FormAction {
  name: string; // e.g., 'save', 'edit', 'delete'
  label: string; // UI label for the button
  component: 'button' | 'dropdown' | 'switch';
  icon?: React.ReactNode; // Icon for the button
  handler: () => any; // Action logic
  isAllowed: boolean; // Permission check
  isVisible: boolean; // Visibility check based on form state
  value?: boolean; // Value for the switch
}
