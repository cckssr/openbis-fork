// Core types for New Forms V2 - Three-Layer Architecture

export enum FormMode {
  CREATE = 'CREATE',
  EDIT = 'EDIT',
  VIEW = 'VIEW',
  DELETE = 'DELETE'
}

export interface FormData {
  [key: string]: any;
}

export interface FormSchema {
  entityType: string;
  sections: FormSection[];
  fields: { [key: string]: FormField };
  validation?: ValidationRules;
}

export interface FormSection {
  id: string;
  title: string;
  description?: string;
  fields: string[];
  collapsible?: boolean;
  collapsed?: boolean;
  order: number;
}

export interface FormField {
  id: string;
  type: string;
  label: string;
  required?: boolean;
  disabled?: boolean;
  hidden?: boolean;
  placeholder?: string;
  validation?: FieldValidation;
  dependencies?: FieldDependency[];
  order: number;
  section: string;
}

export interface FieldValidation {
  required?: boolean;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  custom?: (value: any) => string | null;
}

export interface FieldDependency {
  field: string;
  condition: (value: any) => boolean;
  action: 'show' | 'hide' | 'enable' | 'disable' | 'require' | 'optional';
}

export interface ValidationRules {
  [fieldId: string]: FieldValidation;
}

export interface ValidationState {
  [fieldId: string]: {
    isValid: boolean;
    error?: string;
  };
}

export interface ValidationResult {
  isValid: boolean;
  errors: { [fieldId: string]: string };
}

export interface Permissions {
  canRead: boolean;
  canWrite: boolean;
  canCreate: boolean;
  canDelete: boolean;
  canAdmin: boolean;
}

export interface FormContext {
  entityType: string;
  entityId: string;
  mode: FormMode;
  user: any;
  openbisFacade: any;
  metadata?: any;
  permissions?: Permissions;
}

export interface EntityFormConfig {
  formId: string;
  entityType: string;
  entityId: string;
  mode: FormMode;
  data: FormData;
  schema: FormSchema;
  controller: any;
  metadata: any;
  permissions: Permissions;
}

export interface FormCallbacks {
  onSave?: (result: any) => void;
  onCancel?: () => void;
  onDelete?: (entityId: string) => void;
  onError?: (error: Error) => void;
}

export interface FormState {
  entityType: string;
  entityId: string;
  mode: FormMode;
  data: FormData;
  schema: FormSchema;
  validation: ValidationState;
  isDirty: boolean;
  isValid: boolean;
  isLoading: boolean;
}

export interface FormStore {
  forms: { [formId: string]: FormState };
  activeFormId: string | null;
  
  // Entity-specific state
  entityControllers: { [formId: string]: any };
  entityMetadata: { [formId: string]: any };
  entityPermissions: { [formId: string]: Permissions };
  
  // Actions
  createEntityForm: (config: EntityFormConfig) => void;
  setEntityController: (formId: string, controller: any) => void;
  setEntityMetadata: (formId: string, metadata: any) => void;
  setEntityPermissions: (formId: string, permissions: Permissions) => void;
  updateFormData: (formId: string, data: Partial<FormData>) => void;
  setFormMode: (formId: string, mode: FormMode) => void;
  validateForm: (formId: string) => ValidationResult;
  saveForm: (formId: string) => Promise<void>;
  deleteForm: (formId: string) => void;
  setActiveForm: (formId: string) => void;
  setFormLoading: (formId: string, isLoading: boolean) => void;
  setFormDirty: (formId: string, isDirty: boolean) => void;
  setFormValid: (formId: string, isValid: boolean) => void;
}

export interface FormDispatcherProps {
  entityType: string;
  entityId: string;
  mode: FormMode;
  user: any;
  openbisFacade: any;
  onSave?: (result: any) => void;
  onCancel?: () => void;
  onDelete?: (entityId: string) => void;
}

export interface EntityFormRendererProps {
  entityId: string;
  mode: FormMode;
  user: any;
  openbisFacade: any;
  onSave?: (result: any) => void;
  onCancel?: () => void;
  onDelete?: (entityId: string) => void;
}

export interface BaseFormController {
  init(entityId: string, mode: FormMode): Promise<void>;
  load(entityId: string): Promise<FormData>;
  save(data: FormData): Promise<void>;
  checkPermissions(entityId: string): Promise<Permissions>;
  delete(entityId: string): Promise<void>;
  loadMetadata(entityId: string): Promise<any>;
  getChildren(entityId: string): Promise<any[]>;
  getHistory(entityId: string): Promise<any[]>;
}

export interface BaseFormModel {
  entityType: string;
  baseSchema: FormSchema;
  registerFields(): Promise<void>;
  enhanceSchema(baseSchema: FormSchema, context: FormContext): FormSchema;
  validate(data: FormData): ValidationResult;
}