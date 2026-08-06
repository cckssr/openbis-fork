import { useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';

interface UseConflictFlowParams {
  form: Form | null;
  restoreForm: (form: Form) => void;
  closeConflictDialog: () => void;
  setConflictResolving: (isResolving: boolean) => void;
}

/**
 * Hook to encapsulate conflict resolution dialog flow
 * Handles resolving conflicts by updating form fields with chosen values
 */
export const useConflictFlow = ({
  form,
  restoreForm,
  closeConflictDialog,
  setConflictResolving,
}: UseConflictFlowParams) => {
  const handleResolveConflicts = useCallback(
    async (resolved: Record<string, any>) => {
      if (form) {
        restoreForm({
          ...form,
          fields: form.fields.map(field =>
            resolved.hasOwnProperty(field.id) ? { ...field, value: resolved[field.id] } : field
          ),
        });
      }
      closeConflictDialog();
      setConflictResolving(true);
    },
    [form, restoreForm, closeConflictDialog, setConflictResolving]
  );

  return {
    handleResolveConflicts,
  };
};

