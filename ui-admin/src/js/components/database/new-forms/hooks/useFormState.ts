import { useState, useCallback, useMemo } from 'react';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';

interface UseFormStateProps {
  initialForm: Form | null;
  initialMode: FormMode;
}

interface UseFormStateReturn {
  form: Form | null;
  originalForm: Form | null; // Original form state for comparison
  mode: FormMode;
  isDirty: boolean;
  isValid: boolean;
  updateField: (fieldId: string, value: any) => void;
  updateFieldMetadata: (fieldId: string, meta: any) => void;
  setMode: (mode: React.SetStateAction<FormMode>) => void;
  setForm: (form: React.SetStateAction<Form | null>) => void;
  restoreForm: (form: Form) => void;
  resetForm: () => void;
}

export const useFormState = ({ 
  initialForm, 
  initialMode 
}: UseFormStateProps): UseFormStateReturn => {
  const [form, setForm] = useState<Form | null>(initialForm);
  const [mode, setMode] = useState<FormMode>(initialMode);
  const [originalForm, setOriginalForm] = useState<Form | null>(initialForm);

  // A direct value passed to `setForm` (e.g. after loading from the server, or resetting to the
  // original state on Cancel) represents a new baseline, so `originalForm` is updated to match.
  // A function updater represents an incremental change applied on top of the current form (the
  // documented pattern for "complex updates", e.g. a bulk field edit from a custom action) and
  // must NOT touch `originalForm` - doing so would make the change invisible to dirty-tracking
  // (`isDirty`, auto-save's dirty-field diff), silently treating a real unsaved edit as pristine.
  const handleSetForm = useCallback((newForm: React.SetStateAction<Form | null>) => {
    if (typeof newForm === 'function') {
      setForm(newForm);
    } else {
      setForm(newForm);
      if (newForm) {
        setOriginalForm(newForm);
      }
    }
  }, []);

  const restoreForm = useCallback((restoredForm: Form) => {
    setForm(restoredForm);
  }, []);

  // Update field value
  const updateField = useCallback((fieldId: string, value: any) => {
    // console.log(`[useFormState] Updating field: ${fieldId} to ${value} of type ${typeof value}`);
    setForm(prevForm => {
      if (!prevForm) return null;
      
      return {
        ...prevForm,
        fields: prevForm.fields.map(currentField => {
					if (currentField.id === fieldId) {
						return { ...currentField, value };
					}
					return currentField;
				}),
        isDirty: true
      };
    });
  }, []);

  const updateFieldMetadata = useCallback((fieldId: string, meta: any) => {
    setForm(prevForm => {
      if (!prevForm) return null;
      
      return {
        ...prevForm,
        fields: prevForm.fields.map(currentField => {
          if (currentField.id === fieldId) {
            return { ...currentField, meta };
          }
          return currentField;
        }),
        isDirty: true
      };
    });
  }, []);

  // Reset form to original state
  const resetForm = useCallback(() => {
    setForm(originalForm);
    setMode(FormMode.VIEW);
  }, [originalForm]);

  // Calculate if form is dirty
  const isDirty = useMemo(() => {
    if (!form || !originalForm) return false;
    
    return JSON.stringify(form.fields) !== JSON.stringify(originalForm.fields);
  }, [form, originalForm]);

  // Calculate if form is valid
  const isValid = useMemo(() => {
    if (!form) return false;
    
    return form.fields.every(field => {
      if (!field.required) return true;
      return field.value !== null && field.value !== undefined && field.value !== '';
    });
  }, [form]);

  return {
    form,
    originalForm,
    mode,
    isDirty,
    isValid,
    updateField,
    updateFieldMetadata,
    setMode,
    setForm: handleSetForm,
    restoreForm,
    resetForm
  };
};
