import React, { useState, useMemo, useEffect, useCallback } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import ControllerDispatcher from '@src/js/components/database/new-forms/engine/ControllerDispatcher.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { useFormState } from '@src/js/components/database/new-forms/hooks/useFormState.ts';
import { useOperationState } from '@src/js/components/database/new-forms/hooks/useOperationState.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';


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

  
  // Other state (could also be extracted if needed)
  const [permissions] = useState({ canEdit: true, canDelete: true, canMove: true });
  
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
          const loadedForm = await controller.load(permId, entityKind, params, 'ENTRY');
          setForm(loadedForm);
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
        onAction={() => alert('actions not implemented')}
        params={{ sessionID: sessionID }}
      />
    </>
  );
};