import { useState, useCallback, useMemo } from 'react';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';

interface UseFormStateProps {
  initialForm: Form | null;
  initialMode: FormMode;
}

interface UseFormStateReturn {
  form: Form | null;
  mode: FormMode;
  isDirty: boolean;
  isValid: boolean;
  updateField: (fieldId: string, value: any) => void;
  setMode: (mode: React.SetStateAction<FormMode>) => void;
  setForm: (form: React.SetStateAction<Form | null>) => void;
  resetForm: () => void;
}

export const useFormState = ({ 
  initialForm, 
  initialMode 
}: UseFormStateProps): UseFormStateReturn => {
  const [form, setForm] = useState<Form | null>(initialForm);
  const [mode, setMode] = useState<FormMode>(initialMode);
  const [originalForm, setOriginalForm] = useState<Form | null>(initialForm);

  // Update original form when form changes externally
  const handleSetForm = useCallback((newForm: React.SetStateAction<Form | null>) => {
    setForm(newForm);
    if (typeof newForm === 'function') {
      // Handle function updates
      setForm(prevForm => {
        const updatedForm = newForm(prevForm);
        if (updatedForm) {
          setOriginalForm(updatedForm);
        }
        return updatedForm;
      });
    } else if (newForm) {
      setOriginalForm(newForm);
    }
  }, []);

  // Update field value
  const updateField = useCallback((fieldId: string, value: any) => {
    setForm(prevForm => {
      if (!prevForm) return null;
      
      return {
        ...prevForm,
        fields: prevForm.fields.map(field => 
          field.id === fieldId ? { ...field, value } : field
        ),
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
    mode,
    isDirty,
    isValid,
    updateField,
    setMode,
    setForm: handleSetForm,
    resetForm
  };
};
