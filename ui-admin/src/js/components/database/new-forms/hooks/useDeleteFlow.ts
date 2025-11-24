import { useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { DialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';

interface UseDeleteFlowParams {
  form: Form | null;
  controller: IFormController;
  entityKind: string;
  dialogs: DialogState;
  getExtendedActionContext: (reason?: string) => IExtendedActionContext;
  openDeleteDialog: (config: any) => void;
  closeDeleteDialog: () => void;
  setSaving: (saving: boolean) => void;
  setError: (error: any) => void;
  clearError: () => void;
  executeOperation: <T,>(
    operation: () => Promise<T>,
    options?: { setLoading?: boolean; setSaving?: boolean }
  ) => Promise<T | null>;
  externalAppController?: any;
}

/**
 * Hook to encapsulate delete dialog flow
 * Handles dependency checking, dialog opening, and actual deletion
 */
export const useDeleteFlow = ({
  form,
  controller,
  entityKind,
  dialogs,
  getExtendedActionContext,
  openDeleteDialog,
  closeDeleteDialog,
  setSaving,
  setError,
  clearError,
  executeOperation,
  externalAppController,
}: UseDeleteFlowParams) => {
  const handleDeleteWithDependencyCheck = useCallback(async () => {
    if (!form || !controller) {
      return;
    }

    await executeOperation(
      async () => {
        // Check for existing deletions in trashcan
        try {
          await controller.delete(form, { checkOnly: true });
        } catch (deletionError: any) {
          throw new Error(deletionError.message);
        }

        // Check for dependent entities
        const dependentEntities = await controller.getDependentEntities(form);
        console.log('handleDeleteWithDependencyCheck.dependentEntities:', dependentEntities);
        const totalDependentEntities =
          dependentEntities.experiments.length + dependentEntities.samples.length;

        const deleteConfig = {
          includeReason: true,
          numberOfEntities: totalDependentEntities,
          bypassesTrashcan: totalDependentEntities === 0,
          dependentEntities: dependentEntities,
          entityKind: entityKind,
        };

        openDeleteDialog(deleteConfig);
      },
      { setLoading: true }
    );
  }, [
    form,
    controller,
    entityKind,
    openDeleteDialog,
    executeOperation,
  ]);

  const handleDeleteConfirm = useCallback(
    async (reason: string) => {
      closeDeleteDialog();
      setSaving(true);
      clearError();

      if (form) {
        const context: IExtendedActionContext = getExtendedActionContext(reason);
        try {
          await controller.delete(form, context);
          externalAppController?.closeForm({
            type: context.form.entityType,
            id: context.form.entityPermId,
          });
        } catch (error: any) {
          setError(error.message ?? error);
        } finally {
          setSaving(false);
        }
      }
    },
    [
      form,
      controller,
      getExtendedActionContext,
      closeDeleteDialog,
      setSaving,
      clearError,
      setError,
      externalAppController,
    ]
  );

  const handleDeleteCancel = useCallback(() => {
    closeDeleteDialog();
  }, [closeDeleteDialog]);

  return {
    handleDeleteWithDependencyCheck,
    handleDeleteConfirm,
    handleDeleteCancel,
  };
};
