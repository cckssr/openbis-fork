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
import RestoreDraftDialog from '@src/js/components/database/new-forms/components/common/RestoreDraftDialog.tsx';
import UnsavedChangesDialog from '@src/js/components/common/dialog/UnsavedChangesDialog.jsx';

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
  onUnsavedStateChange,
}: {
  openbisFacade: any;
  params: any;
  entityKind: string;
  user: string;
  sessionID: string;
  permId: string;
  initialMode: FormMode;
  externalAppController: any;
  onUnsavedStateChange?: (changed: boolean) => void;
}) => {

  const actionToastContext = useActionToastCtx();
  // ErrorDialog is a .jsx component without TS typings; cast to any to satisfy TSX type checking.
  const ErrorDialogAny = ErrorDialog as any;
  // UnsavedChangesDialog is a .jsx component without TS typings; cast to any.
  const UnsavedChangesDialogAny = UnsavedChangesDialog as any;

  // Form state (already well-organized)
  const { form, originalForm, mode, setForm, restoreForm, setMode, updateField, updateFieldMetadata } = useFormState({
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

  // Other state (could also be extracted if needed)
  const [permissions] = useState({ canEdit: true, canDelete: true, canMove: true });

  // Confirmation dialog for cancelling unsaved changes (driven by CoreFormModel cancel actions)
  const [unsavedConfirm, setUnsavedConfirm] = useState<{
    open: boolean;
    onConfirm: (() => void | Promise<void>) | null;
  }>({ open: false, onConfirm: null });

  const requestUnsavedConfirmation = useCallback(
    (onConfirm: () => void | Promise<void>) => {
      setUnsavedConfirm({ open: true, onConfirm });
    },
    []
  );

  // Handle data restoration from localStorage.
  const handleDataRestore = useCallback((savedData: Form) => {
    restoreForm(savedData);
    actionToastContext.raiseInfo('Restored unsaved changes');
  }, [restoreForm, actionToastContext]);

  // Handle auto-save being forced off because another new entity of the same type already
  // owns the (shared) CREATE-mode auto-save slot.
  const handleAutoSaveBlocked = useCallback((message: string) => {
    actionToastContext.raiseWarning(message);
  }, [actionToastContext]);

  // Auto-save feature flow (preference + save + restore + actionOverrides)
  const {
    isAutoSaveEnabled,
    setAutoSaveEnabled,
    actionOverrides,
    clearStorage,
    hasPendingDraft,
    restorePendingDraft,
    discardPendingDraft,
    dismissPendingDraft
  } = useEntityAutoSaveFlow({
    form,
    originalForm,
    mode,
    user,
    entityKind,
    entityType: params?.entityType || '',
    permId,
    onRestore: handleDataRestore,
    onAutoSaveBlocked: handleAutoSaveBlocked
  });

  // Report unsaved-changes state to the tab system (drives the close-tab warning via
  // tab.changed) and to the parent (Files-tab guard).
  // Drafts are persisted to localStorage in both EDIT and CREATE mode when auto-save is
  // enabled (see useEntityAutoSaveFlow), so only warn about losing changes when it's off.
  useEffect(() => {
    const changed =
      (mode === FormMode.EDIT || mode === FormMode.CREATE) && !isAutoSaveEnabled;
    externalAppController.objectChange({ objectTypeChanging: entityKind, id: permId, changed });
    if (onUnsavedStateChange) {
      onUnsavedStateChange(changed);
    }
  }, [mode, isAutoSaveEnabled, entityKind, permId]);

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
      requestUnsavedConfirmation,
      deleteReason: reason || undefined,
      dependentEntities: dialogs.delete.config?.dependentEntities || undefined,
    };
  }, [form, mode, externalAppController, controller, dialogs.delete.config, isAutoSaveEnabled, setAutoSaveEnabled, requestUnsavedConfirmation]);

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
    restoreForm,
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

  // Handle actions by creating them from a dispatcher
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
      <UnsavedChangesDialogAny
        open={unsavedConfirm.open}
        onConfirm={async () => {
          const cb = unsavedConfirm.onConfirm;
          setUnsavedConfirm({ open: false, onConfirm: null });
          if (cb) {
            try {
              await cb();
            } catch (e: any) {
              setError(getErrorMessage(e, 'Action failed'));
              console.error(formatErrorForLogging(e, 'EntityFormContextProvider.unsavedConfirm'));
            }
          }
        }}
        onCancel={() => setUnsavedConfirm({ open: false, onConfirm: null })}
      />
      <RestoreDraftDialog
        open={hasPendingDraft}
        onRestore={restorePendingDraft}
        onDiscard={discardPendingDraft}
        onDismiss={dismissPendingDraft}
      />
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