import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { FormFieldDataType, FormMode, FormSection, EntityKind } from '@src/js/components/database/new-forms/types/form.enums.ts';

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
  sections?: SectionGroup[];
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
  handler?: (...args: any[]) => void;
  isAllowed: boolean;
  //isVisible: boolean;
  visibility: VisibilityRule[];
  value?: any;
}

export interface SectionGroup {
  section: string;
  fields: string[];
}

export interface IBaseActionContext {
  controller: IFormController;
  form: Form;
  setForm: React.Dispatch<React.SetStateAction<Form | null>>;
}

export interface IModeActionContext extends IBaseActionContext {
  mode: FormMode;
  setMode: React.Dispatch<React.SetStateAction<FormMode>>;
}

export interface IExtendedActionContext extends IModeActionContext {
  externalAppController: any;
  onAfterSave: (params?: any) => void;
  deleteReason?: string;
  dependentEntities?: any;
}

export interface IAutoSaveActionContext extends IBaseActionContext {
  isAutoSaveEnabled: boolean;
  setAutoSaveEnabled: (isAutoSaveEnabled: boolean) => void;
}

// Context object for actions
/* export interface ActionContext {
  controller: IFormController;
  form: Form;
  setForm: React.Dispatch<React.SetStateAction<Form | null>>;
  mode: FormMode;
  setMode: React.Dispatch<React.SetStateAction<FormMode>>;
  permissions: any;
  onAfterSave: (params?: any) => void;
  openbisFacade: any; // Provide external dependencies
  externalAppController: any;
  isAutoSaveEnabled: boolean;
  setAutoSaveEnabled: (isAutoSaveEnabled: boolean) => void;
  deleteReason?: string; // Optional reason for delete operations
  dependentEntities?: any; // Optional dependent entities info for delete operations
} */

export interface FieldRendererProps {
  field: FormField;
  onFieldChange: (fieldId: string, value: any) => void;
  onFieldMetadataChange?: (fieldId: string, meta: any) => void;
  mode: FormMode;
  params?: any;
}

export interface ActionRendererProps {
  action: FormAction;
  onAction: (actionName: string) => void;
  mode: FormMode;
}