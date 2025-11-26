import React, { useState, useMemo, useEffect, useCallback } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';

import ControllerDispatcher from '@src/js/components/database/new-forms/engine/ControllerDispatcher.ts';
import ActionHandlerDispatcher from '@src/js/components/database/new-forms/engine/ActionHandlerDispatcher.ts';

import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';

import { useFormState } from '@src/js/components/database/new-forms/hooks/useFormState.ts';
import { useOperationState } from '@src/js/components/database/new-forms/hooks/useOperationState.ts';
import { useDialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';
import { useConflictFlow } from '@src/js/components/database/new-forms/hooks/useConflictFlow.ts';

import { findConflicts, checkModificationDateConflict } from '@src/js/components/database/new-forms/utils/conflictResolutionUtil.ts';
import ConflictResolutionDialog from '@src/js/components/database/new-forms/components/common/ConflictResolutionDialog.tsx';


export const EntityFormContextProvider = ({
  openbisFacade,
  params,
  entityKind,
  user,
  sessionID,
  permId,
  initialMode,
  externalAppController
}: {
  openbisFacade: any;
  params: any;
  entityKind: string;
  user: string;
  sessionID: string;
  permId: string;
  initialMode: FormMode;
  externalAppController: any;
}) => {
  // Other state (could also be extracted if needed)
  const [permissions] = useState({ canEdit: true, canDelete: true, canMove: true });
  const [isAutoSaveEnabled] = useState(false);
  // Form state (already well-organized)
  const { form, mode, setForm, setMode, updateField, updateFieldMetadata } = useFormState({
    initialForm: null,
    initialMode
  });

  // Operation state (loading, saving, error) - NEW
  const {
    operationState,
    setLoading, setSaving,
    setError, clearError,
    executeOperation
  } = useOperationState();

  // Dialog state (conflict, delete, move) - NEW
  const {
    dialogs,
    openConflictDialog, closeConflictDialog, setConflictResolving,
    openDeleteDialog, closeDeleteDialog,
    openMoveDialog, closeMoveDialog,
  } = useDialogState();

  const { handleResolveConflicts } = useConflictFlow({
    form,
    setForm,
    closeConflictDialog,
    setConflictResolving,
  });

  /* const { saveToStorage, loadFromStorage, clearStorage } = useAutoSave({
      formData: form,
      storageKey: `form-data-${permId}`,
      isEnabled: isAutoSaveEnabled,
      interval: 5000,
    });
  
    useEffect(() => {
      if (isAutoSaveEnabled) {
        saveToStorage();
      }
    }, [isAutoSaveEnabled, form]); */

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
      onAfterSave: (params?: any) => {
        console.log('[EntityFormContextProvider] context onAfterSave:', params);
        setMode(FormMode.VIEW);
        if (params) {
          externalAppController.objectCreate(params);
        }
        loadForm();
      },
      externalAppController,
      deleteReason: reason || undefined,
      //dependentEntities: dialogs.delete.config?.dependentEntities || undefined,
    };
  }, [form, mode, externalAppController, controller]);

  // Load initial form data
  useEffect(() => {
    loadForm();
  }, [permId, controller]);

  const loadForm = useCallback(async () => {
    console.log('loadForm', { permId }, { entityKind }, { params });

    await executeOperation(
      async () => {
        if (entityKind === EntityKind.NEW_OBJECT) {
          //const loadedForm = await controller.load(permId, entityKind, params, 'ENTRY');
          //setForm(loadedForm);
        } else {
          const loadedForm = await controller.load(permId, entityKind, params);
          setForm(loadedForm);
        }
      },
      { setLoading: true }
    );
  }, [permId, entityKind, params, controller, executeOperation, setForm]);

  const handleErrorCancel = () => {
    clearError();
  };

  // Handle actions by creating them from dispatcher
  const handleAction = useCallback(async (actionName: string) => {
    console.log(`[EntityFormContextProvider] Handling action: ${actionName}`);
    const actionHandler = ActionHandlerDispatcher.getActionHandler(actionName);

    if (!actionHandler || !form) {
      console.warn(`No action handler registered for '${actionName}'`);
      return;
    }

    if (actionName.toLowerCase().includes('save')) {
      await handleSaveActions(actionHandler);
    } /* else if (actionName === 'delete') {
      await handleDeleteWithDependencyCheck();
    } else if (actionName === 'move') {
      await handleMoveRequest();
    } */ else {
      const context: IExtendedActionContext = getExtendedActionContext();
      try {
        await actionHandler(context);
      } catch (e: any) {
        setError(e.message);
      } finally {
        setSaving(false);
      }
    }
  }, [form, mode, permissions, openbisFacade, externalAppController, getExtendedActionContext, setSaving, setError]);

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
          console.log('conflicts', conflicts);
          openConflictDialog(conflicts);
          setSaving(false);
          return;
        }

        await actionHandler(context);
        setConflictResolving(false);
        if (context.onAfterSave) context.onAfterSave(params);
      } catch (error: any) {
        if (error.status === 409) {
          const latestForm = await controller.load(form.entityPermId);
          const conflicts = findConflicts(form, latestForm) as any[];
          openConflictDialog(conflicts);
        } else {
          console.error('Save failed:', error);
          setError(error.message);
        }
      } finally {
        setSaving(false);
      }
    } else if (mode === FormMode.CREATE) {
      try {
        await actionHandler(context);
      } catch (error: any) {
        setError(error.message);
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
        <ErrorDialog
          key='entity-form-error-dialog'
          open={!!operationState.error}
          error={operationState.error}
          onClose={handleErrorCancel}
        />
      )}
      <EntityForm
        form={form}
        mode={mode}
        permissions={permissions}
        onFieldChange={updateField}
        onFieldMetadataChange={updateFieldMetadata}
        onAction={handleAction}
        params={{ sessionID: sessionID }}
      />
      {dialogs.conflict.isOpen && (
        <ConflictResolutionDialog
          open={dialogs.conflict.isOpen}
          conflicts={dialogs.conflict.fields}
          onResolve={handleResolveConflicts}
          onCancel={closeConflictDialog}
        />
      )}
    </>
  );
};