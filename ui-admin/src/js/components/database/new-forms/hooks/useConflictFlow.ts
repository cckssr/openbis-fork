import { useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { DialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';

interface UseConflictFlowParams {
  form: Form | null;
  setForm: (updater: (prevForm: Form | null) => Form | null) => void;
  closeConflictDialog: () => void;
  setConflictResolving: (isResolving: boolean) => void;
}

/**
 * Hook to encapsulate conflict resolution dialog flow
 * Handles resolving conflicts by updating form fields with chosen values
 */
export const useConflictFlow = ({
  form,
  setForm,
  closeConflictDialog,
  setConflictResolving,
}: UseConflictFlowParams) => {
  const handleResolveConflicts = useCallback(
    async (resolved: Record<string, any>) => {
      setForm(prevForm => {
        if (!prevForm) return null;
        return {
          ...prevForm,
          fields: prevForm.fields.map(field =>
            resolved.hasOwnProperty(field.id) ? { ...field, value: resolved[field.id] } : field
          ),
        };
      });
      closeConflictDialog();
      setConflictResolving(true);
    },
    [setForm, closeConflictDialog, setConflictResolving]
  );

  return {
    handleResolveConflicts,
  };
};

