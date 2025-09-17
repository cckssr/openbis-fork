// useFormEngine - Simplified hook for New Forms V2

import { useCallback } from 'react';
import { useFormStore } from '@src/js/components/database/new-forms-v2/core/stores/formStore.ts';
import { FormCallbacks, ValidationResult } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export const useFormEngine = (formId: string, callbacks: FormCallbacks) => {
  const form = useFormStore(state => state.forms[formId]);
  const controller = useFormStore(state => state.entityControllers[formId]);
  const metadata = useFormStore(state => state.entityMetadata[formId]);
  const permissions = useFormStore(state => state.entityPermissions[formId]);
  
  const updateFormData = useFormStore(state => state.updateFormData);
  const validateForm = useFormStore(state => state.validateForm);
  const saveForm = useFormStore(state => state.saveForm);
  const deleteForm = useFormStore(state => state.deleteForm);
  const setFormLoading = useFormStore(state => state.setFormLoading);
  const setFormDirty = useFormStore(state => state.setFormDirty);
  const setFormValid = useFormStore(state => state.setFormValid);

  if (!form || !controller) {
    throw new Error(`Form ${formId} not found or not initialized`);
  }

  const updateField = useCallback((fieldId: string, value: any) => {
    updateFormData(formId, { [fieldId]: value });
  }, [formId, updateFormData]);

  const validateField = useCallback((fieldId: string): ValidationResult => {
    const field = form.schema.fields[fieldId];
    if (!field) {
      return { isValid: true, errors: {} };
    }

    const value = form.data[fieldId];
    const errors: { [fieldId: string]: string } = {};
    let isValid = true;

    if (field.validation?.required && (!value || value === '')) {
      errors[fieldId] = `${field.label} is required`;
      isValid = false;
    }

    return { isValid, errors };
  }, [form, formId]);

  const saveFormData = useCallback(async (): Promise<void> => {
    try {
      setFormLoading(formId, true);
      await saveForm(formId);
      setFormDirty(formId, false);
    } catch (error) {
      console.error('Save failed:', error);
      throw error;
    } finally {
      setFormLoading(formId, false);
    }
  }, [formId, saveForm, setFormLoading, setFormDirty]);

  return {
    form,
    controller,
    metadata,
    permissions,
    data: form.data,
    schema: form.schema,
    validation: form.validation,
    isDirty: form.isDirty,
    isValid: form.isValid,
    isLoading: form.isLoading,
    mode: form.mode,
    entityType: form.entityType,
    entityId: form.entityId,
    updateField,
    validateField,
    validateForm: () => validateForm(formId),
    saveForm: saveFormData,
    deleteForm: () => deleteForm(formId),
    onSave: callbacks.onSave,
    onCancel: callbacks.onCancel,
    onDelete: callbacks.onDelete,
    onError: callbacks.onError,
  };
};