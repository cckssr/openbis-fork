import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';

export interface FormState {
  form: Form | null;
  mode: FormMode;
  isDirty: boolean;
  isValid: boolean;
  loading: boolean;
  saving: boolean;
  error: string | null;
}

export interface FormActions {
  updateField: (fieldId: string, value: any) => void;
  setMode: (mode: FormMode) => void;
  setForm: (form: Form | null) => void;
  resetForm: () => void;
  executeAction: (actionName: string) => Promise<void>;
  loadForm: () => Promise<void>;
  clearError: () => void;
}

export interface FormPermissions {
  canEdit: boolean;
  canDelete: boolean;
  canMove: boolean;
  loading?: boolean;
  error?: string | null;
}

export interface FormContextValue {
  state: FormState;
  actions: FormActions;
  permissions: FormPermissions;
  // Conflict resolution
  conflicts: Conflict[];
  showConflictDialog: boolean;
  resolveConflicts: (resolutions: Record<string, 'local' | 'server' | 'custom'>) => void;
  setShowConflictDialog: (show: boolean) => void;
}

export interface Conflict {
  fieldId: string;
  fieldName: string;
  localValue: any;
  serverValue: any;
  localField: any;
  serverField: any;
}
