import { useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { DialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';
import { getErrorMessage, formatErrorForLogging } from '@src/js/components/database/new-forms/utils/errorUtil.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';

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
  actionToastContext?: any;
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
  actionToastContext,
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
      const errorMessage = getErrorMessage(error, 'Failed to prepare move operation');
      setError(errorMessage);
      console.error(formatErrorForLogging(error, 'useMoveFlow.handleMoveRequest'));
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

      try {
        if (moveResult && moveResult.success) {
          setSaving(true);
          clearError();

          await loadForm();
          actionToastContext?.raiseSuccess(`Entity successfully moved to ${moveResult.targetKind}: ${moveResult.targetIdentifier} `);
          if (externalAppController?.objectMove) {
            externalAppController.objectMove({
              type: form.entityKind == EntityKind.SAMPLE ? EntityKind.OBJECT : form.entityKind
            });
          }
        } else {
          // noinspection ExceptionCaughtLocallyJS
          throw moveResult.error;
        }
      } catch (error: any) {
        const errorMessage = getErrorMessage(error, 'Failed to move entity');
        setError(errorMessage);
        console.error(formatErrorForLogging(error, 'useMoveFlow.handleMoveConfirm'));
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

