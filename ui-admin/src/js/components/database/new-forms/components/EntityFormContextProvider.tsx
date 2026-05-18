import React, { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';

import ControllerDispatcher from '@src/js/components/database/new-forms/engine/ControllerDispatcher.ts';
import ActionHandlerDispatcher from '@src/js/components/database/new-forms/engine/ActionHandlerDispatcher.ts';

import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';

import { findConflicts, checkModificationDateConflict } from '@src/js/components/database/new-forms/utils/conflictResolutionUtil.ts';
import { getErrorMessage, formatErrorForLogging } from '@src/js/components/database/new-forms/utils/errorUtil.ts';
import ConflictResolutionDialog from '@src/js/components/database/new-forms/components/common/ConflictResolutionDialog.tsx';
import DeleteConfirmationDialog from '@src/js/components/database/new-forms/components/common/DeleteConfirmationDialog.tsx';
import MoveDialog from '@src/js/components/database/new-forms/components/common/MoveDialog.tsx';
import EntityTypeSelectionDialog from '@src/js/components/database/new-forms/components/common/EntityTypeSelectionDialog.tsx';

import { useFormState } from '@src/js/components/database/new-forms/hooks/useFormState.ts';
import { useOperationState } from '@src/js/components/database/new-forms/hooks/useOperationState.ts';
import { useDialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';

import { useMoveFlow } from '@src/js/components/database/new-forms/hooks/useMoveFlow.ts';
import { useConflictFlow } from '@src/js/components/database/new-forms/hooks/useConflictFlow.ts';
import { useDeleteFlow } from '@src/js/components/database/new-forms/hooks/useDeleteFlow.ts';
import { useEntityAutoSaveFlow } from '@src/js/components/database/new-forms/hooks/useEntityAutoSaveFlow.tsx';

import { ActionToast, useActionToastCtx } from '@src/js/components/database/new-forms/components/common/ActionToast.tsx';

export const EntityFormContextProvider = ({
  openbisFacade,
  params,
  entityKind,
  user,
  sessionID,
  permId,
  initialMode,
  externalAppController,
  onModeChange,
}: {
  openbisFacade: any;
  params: any;
  entityKind: string;
  user: string;
  sessionID: string;
  permId: string;
  initialMode: FormMode;
  externalAppController: any;
  onModeChange?: (mode: FormMode) => void;
}) => {

  const actionToastContext = useActionToastCtx();
  // ErrorDialog is a .jsx component without TS typings; cast to any to satisfy TSX type checking.
  const ErrorDialogAny = ErrorDialog as any;

  // Form state (already well-organized)
  const { form, originalForm, mode, setForm, setMode, updateField, updateFieldMetadata } = useFormState({
    initialForm: null,
    initialMode
  });

  // Operation state (loading, saving, error)
  const {
    operationState,
    setLoading, setSaving,
    setError, clearError,
  } = useOperationState();

  // Dialog state (conflict, delete, move) - NEW
  const {
    dialogs,
    openNewDialog, closeNewDialog,
    openConflictDialog, closeConflictDialog, setConflictResolving,
    openDeleteDialog, closeDeleteDialog,
    openMoveDialog, closeMoveDialog,
  } = useDialogState();

  // Notify parent when mode changes (e.g., to show unsaved-changes warning on tab switch)
  useEffect(() => {
    if (onModeChange) {
      onModeChange(mode);
    }
  }, [mode]);

  // Other state (could also be extracted if needed)
  const [permissions] = useState({ canEdit: true, canDelete: true, canMove: true });

  // Handle data restoration from localStorage
  const handleDataRestore = useCallback((savedData: Form) => {
    setForm(savedData);
    actionToastContext.raiseInfo('Restored unsaved changes');
  }, [setForm, actionToastContext]);

  // Auto-save feature flow (preference + save + restore + actionOverrides)
  const {
    isAutoSaveEnabled,
    setAutoSaveEnabled,
    actionOverrides,
    clearStorage
  } = useEntityAutoSaveFlow({
    form,
    originalForm,
    mode,
    user,
    entityKind,
    permId,
    onRestore: handleDataRestore
  });

  // Create controller using dispatcher
  const controller: IFormController = useMemo(
    () => ControllerDispatcher.createController(entityKind, openbisFacade, user),
    [entityKind, openbisFacade, user]
  );

  const getExtendedActionContext = useCallback((reason?: string): IExtendedActionContext => {
    if (!form) throw new Error('Form is not loaded');
    return {
      controller,
      form,
      setForm,
      mode,
      setMode,
      isAutoSaveEnabled,
      setAutoSaveEnabled,
      onAfterSave: (params?: any) => {
        console.log('[EntityFormContextProvider] context onAfterSave:', params);
        setMode(FormMode.VIEW);
        if (params) {
          externalAppController.objectCreate(params);
        } else {
          loadForm();
        }
        setSaving(false);
      },
      externalAppController,
      deleteReason: reason || undefined,
      dependentEntities: dialogs.delete.config?.dependentEntities || undefined,
    };
  }, [form, mode, externalAppController, controller, dialogs.delete.config, isAutoSaveEnabled, setAutoSaveEnabled]);

  // Load initial form data
  useEffect(() => {
    loadForm();
  }, [permId, controller]);

  const loadForm = useCallback(async () => {
    console.log('[EntityFormContextProvider] loadForm', { permId }, { entityKind }, { params });

    setLoading(true);
    clearError();

    try {
      const loadedForm = await controller.load(permId, entityKind, params);
      //console.log('loadedForm: ', loadedForm);
      setForm(loadedForm);
    } catch (error: any) {
      const errorMessage = getErrorMessage(error, 'Failed to load form');
      setError(errorMessage);
      console.error(formatErrorForLogging(error, 'EntityFormContextProvider.loadForm'));
    } finally {
      setLoading(false);
    }
  }, [permId, entityKind, params, controller, setLoading, clearError, setError, setForm]);

  const handleErrorCancel = () => {
    clearError();
  };

  const { handleMoveRequest, handleMoveConfirm, handleMoveCancel } = useMoveFlow({
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
  });

  const { handleResolveConflicts } = useConflictFlow({
    form,
    setForm,
    closeConflictDialog,
    setConflictResolving,
  });

  const { handleDeleteWithDependencyCheck, handleDeleteConfirm, handleDeleteCancel } = useDeleteFlow({
    form,
    controller,
    entityKind,
    dialogs,
    getExtendedActionContext,
    openDeleteDialog,
    closeDeleteDialog,
    setLoading,
    setSaving,
    setError,
    clearError,
    externalAppController,
    actionToastContext,
  });

  // Handle actions by creating them from dispatcher
  const handleAction = useCallback(async (actionName: string) => {
    console.log(`[EntityFormContextProvider] Handling action: ${actionName}`);

    if (!form) {
      console.error('Form is not loaded');
      return;
    }

    if (actionName === 'delete') {
      await handleDeleteWithDependencyCheck();
    } else if (actionName === 'move') {
      await handleMoveRequest();
    } else if (actionName === 'newCollection' || actionName === 'newObject' || actionName === 'newDataSet') {
      openNewDialog(form?.entityKind, actionName);
    } else {

      // Action handlers can require different context shapes (mode, extended, autosave, etc.)
      // so we treat them as dynamic at runtime.
      const actionHandler: any = ActionHandlerDispatcher.getActionHandler(actionName);

      if (!actionHandler) {
        console.warn(`No action handler registered for '${actionName}'`);
        return;
      }

      const lowerActionName = actionName.toLowerCase();
      const isSaveAction = lowerActionName === 'save' || lowerActionName.endsWith(':save');

      if (isSaveAction) {
        await handleSaveActions(actionHandler);
      } else {
        const context: IExtendedActionContext = getExtendedActionContext();
        try {
          await actionHandler(context);
        } catch (e: any) {
          const errorMessage = getErrorMessage(e, 'Action failed');
          setError(errorMessage);
          console.error(formatErrorForLogging(e, `EntityFormContextProvider.handleAction.${actionName}`));
        } finally {
          setSaving(false);
        }
      }
    }
  }, [form, mode, permissions, openbisFacade, externalAppController, getExtendedActionContext, setSaving, setError, handleMoveRequest, handleDeleteWithDependencyCheck, clearStorage]);

  const handleSaveActions = async (actionHandler: any) => {
    if (!form) throw new Error('Form is not loaded');

    const context: IExtendedActionContext = getExtendedActionContext();
    setSaving(true);
    clearError();

    if (mode === FormMode.EDIT) {
      try {
        const latestForm = await controller.load(form.entityPermId);

        if (!dialogs.conflict.isResolving && checkModificationDateConflict(form, latestForm)) {
          const conflicts = findConflicts(form, latestForm) as any[];
          openConflictDialog(conflicts);
          setSaving(false);
          return;
        }

        await actionHandler(context);
        actionToastContext.raiseSuccess('SUCCESSFULLY SAVED ENTITY');

        // Clear saved data after successful save
        clearStorage();

        setConflictResolving(false);
        if (context.onAfterSave) context.onAfterSave(params);
      } catch (error: any) {
        if (error.status === 409) {
          const latestForm = await controller.load(form.entityPermId);
          const conflicts = findConflicts(form, latestForm) as any[];
          openConflictDialog(conflicts);
        } else {
          const errorMessage = getErrorMessage(error, 'Failed to save entity');
          setError(errorMessage);
          actionToastContext.raiseError('ERROR SAVING');
          console.error(formatErrorForLogging(error, 'EntityFormContextProvider.handleSaveActions.EDIT'));
        }
      } finally {
        setSaving(false);
      }
    } else if (mode === FormMode.CREATE) {
      try {
        await actionHandler(context);
        actionToastContext.raiseSuccess('SUCCESSFULLY SAVED ');

        // Clear saved data after successful save
        clearStorage();
      } catch (error: any) {
        const errorMessage = getErrorMessage(error, 'Failed to create entity');
        setError(errorMessage);
        actionToastContext.raiseError('ERROR SAVING ');
        console.error(formatErrorForLogging(error, 'EntityFormContextProvider.handleSaveActions.CREATE'));
      } finally {
        setSaving(false);
      }
    } else {
      throw new Error('Invalid mode');
    }
  };

  // Early returns
  if (operationState.loading) {
    return <LoadingDialog loading={operationState.loading} />;
  }

  if (!form) {
    return null;
  }

  return (
    <>
      {operationState.saving && <LoadingDialog loading={operationState.saving} />}
      {operationState.error && (
        <ErrorDialogAny
          key='entity-form-error-dialog'
          open={!!operationState.error}
          error={operationState.error}
          onClose={handleErrorCancel}
        />
      )}
      <ActionToast ctx={actionToastContext}></ActionToast>
      <EntityForm
        form={form}
        mode={mode}
        permissions={permissions}
        onFieldChange={updateField}
        onFieldMetadataChange={updateFieldMetadata}
        onAction={handleAction}
        params={{ sessionID: sessionID, user: user, entityPermId: form.entityPermId }}
        actionOverrides={actionOverrides}
        openbisFacade={openbisFacade}
      />
      {dialogs.conflict.isOpen && (
        <ConflictResolutionDialog
          open={dialogs.conflict.isOpen}
          conflicts={dialogs.conflict.fields}
          onResolve={handleResolveConflicts}
          onCancel={closeConflictDialog}
        />
      )}
      {dialogs.delete.isOpen && dialogs.delete.config && (
        <DeleteConfirmationDialog
          open={dialogs.delete.isOpen}
          onConfirm={handleDeleteConfirm}
          onCancel={handleDeleteCancel}
          config={dialogs.delete.config}
        />
      )}
      {dialogs.move.isOpen && (
        <MoveDialog
          open={dialogs.move.isOpen}
          onConfirm={handleMoveConfirm}
          onCancel={handleMoveCancel}
          form={form}
          moveInfo={dialogs.move.info}
          openbisFacade={openbisFacade}
          entityFormController={controller}
        />
      )}
      {dialogs.new.isOpen && (
        <EntityTypeSelectionDialog
          open={dialogs.new.isOpen}
          actionName={dialogs.new.actionName || ''}
          onConfirm={async (selectedEntityType: any) => {
            const actionName = dialogs.new.actionName || '';
            const context: IExtendedActionContext = getExtendedActionContext();
            const actionHandler: any = ActionHandlerDispatcher.getActionHandler(actionName);
            if (!actionHandler) {
              console.warn(`No action handler registered for '${actionName}'`);
              return;
            }
            try {
              await actionHandler(context, selectedEntityType.code);
            } catch (e: any) {
              const errorMessage = getErrorMessage(e, 'Failed to create entity');
              setError(errorMessage);
              console.error(formatErrorForLogging(e, 'EntityFormContextProvider.newEntity'));
            } finally {
              setSaving(false);
              closeNewDialog();
            }
          }}
          onCancel={closeNewDialog}
          openbisFacade={openbisFacade}
        />
      )}
    </>
  );
};