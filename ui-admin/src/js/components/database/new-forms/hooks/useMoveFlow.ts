import { useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { DialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';

interface UseMoveFlowParams {
  form: Form | null;
  controller: IFormController;
  dialogs: DialogState;
  openMoveDialog: (info: any) => void;
  closeMoveDialog: () => void;
  loadForm: () => Promise<void>;
  setLoading: (loading: boolean) => void;
  setSaving: (saving: boolean) => void;
  setError: (error: any) => void;
  clearError: () => void;
  externalAppController?: any;
}

export const useMoveFlow = ({
  form,
  controller,
  dialogs,
  openMoveDialog,
  closeMoveDialog,
  loadForm,
  setLoading,
  setSaving,
  setError,
  clearError,
  externalAppController,
}: UseMoveFlowParams) => {
  const handleMoveRequest = useCallback(async () => {
    if (!form || !controller) {
      return;
    }

    try {
      setLoading(true);
      clearError();
      openMoveDialog(form);
    } catch (error: any) {
      setError(error.message ?? error);
    } finally {
      setLoading(false);
    }
  }, [form, controller, setLoading, clearError, openMoveDialog, setError]);

  const handleMoveConfirm = useCallback(
    async (moveResult?: any) => {
      if (!form || !controller) {
        return;
      }

      closeMoveDialog();
      setSaving(true);
      clearError();

      try {
        if (moveResult && moveResult.success) {
          await loadForm();
          if (externalAppController?.objectMove) {
            externalAppController.objectMove({
              type: form.entityType,
              id: form.entityPermId,
              moveInfo: dialogs.move.info,
            });
          }
        } else {
          await loadForm();
        }
      } catch (error: any) {
        console.error('Move failed:', error);
        setError(error.message ?? error);
      } finally {
        setSaving(false);
      }
    },
    [
      form,
      controller,
      closeMoveDialog,
      setSaving,
      clearError,
      loadForm,
      externalAppController,
      dialogs.move.info,
      setError,
    ]
  );

  const handleMoveCancel = useCallback(() => {
    closeMoveDialog();
  }, [closeMoveDialog]);

  return {
    handleMoveRequest,
    handleMoveConfirm,
    handleMoveCancel,
  };
};

