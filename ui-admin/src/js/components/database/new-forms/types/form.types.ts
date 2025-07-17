import { FormController } from '@src/js/components/database/new-forms/entities/FormController.ts';

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
  DATASET = 'DATASET',
}

/**
 * Defines the data type for a form field, mapping to openBIS property types and custom UI widget types.
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

export enum FormSection {
  IDENTIFICATION_INFO = 'Identification Info',
  GENERAL = 'General',
  OVERVIEW = 'Overview'
}

export interface FormFieldMeta {
  isFrozen?: boolean;
  vocabularyOptions?: { code: string; label: string }[];
  [key: string]: any; // For future extensions
}

export interface ValidationRuleDef {
  rule: string; // e.g., 'required', 'minLength'
  message: string;
  [key: string]: any; // e.g., length: 8
}

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
  validation?: ValidationRuleDef[];
}

export interface Form {
  entityPermId: string; // The unique identifier of the entity (permId, code, etc.)
  entityKind: string; // e.g., 'Space', 'Project', 'Sample'
  entityType: string;
  title: string;
  sections: SectionGroup[];
  fields: FormField[];
  version: number;
  mode?: FormMode;
  isDirty: boolean; 
  isValid: boolean; 
  meta: {
    [key: string]: any; // For entity-specific metadata
  };
  actions?: FormAction[];
}

export interface VisibilityRule {
  mode?: FormMode | FormMode[];
  permission?: string;
}

/**
 * Defines the structure for an action that can be performed from the form's toolbar.
 */
export interface FormAction {
  name: string;
  label: string;
  component: 'button' | 'switch' | 'dropdown' | string;
  handler: (...args: any[]) => void;
  isAllowed: boolean;
  //isVisible: boolean;
  visibility: VisibilityRule[];
  value?: any;
}

export interface SectionGroup {
  section: string;
  fields: string[];
}

// NEW: Context object for actions
export interface ActionContext {
  controller: FormController;
  form: Form;
  setForm: React.Dispatch<React.SetStateAction<Form | null>>;
  mode: FormMode;
  setMode: React.Dispatch<React.SetStateAction<FormMode>>;
  permissions: any;
  onAfterSave: () => void;
  openbisFacade: any; // Provide external dependencies
  onNewProject: (spacePermId: string) => void;
  onEntityChange: (permId: string, isNew: boolean) => void;
  closeForm: () => void;
}

// NEW: Props for field renderers
export interface FieldRendererProps {
  field: FormField;
  onFieldChange: (fieldId: string, value: any) => void;
  mode: FormMode;
}