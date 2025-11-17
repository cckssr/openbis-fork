/**
 * REFACTORED VERSION - EXAMPLE
 * 
 * This file demonstrates how EntityFormContextProvider would look
 * with improved state management using custom hooks.
 * 
 * Key improvements:
 * 1. Grouped dialog state via useDialogState
 * 2. Grouped operation state via useOperationState
 * 3. Cleaner, more maintainable code
 * 4. Better separation of concerns
 */

import React, { useState, useMemo, useEffect, useCallback } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import ControllerDispatcher from '@src/js/components/database/new-forms/engine/ControllerDispatcher.ts';
//import ActionHandlerDispatcher from '@src/js/components/database/new-forms/engine/ActionHandlerDispatcher.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/form.types.ts';
//import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';
//import ConflictResolutionDialog from '@src/js/components/database/new-forms/components/common/ConflictResolutionDialog.tsx';
//import DeleteConfirmationDialog from '@src/js/components/database/new-forms/components/common/DeleteConfirmationDialog.tsx';
//import MoveDialog from '@src/js/components/database/new-forms/components/common/MoveDialog.tsx';
import { useFormState } from '@src/js/components/database/new-forms/hooks/useFormState.ts';
//import { useDialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';
//import { useOperationState } from '@src/js/components/database/new-forms/hooks/useOperationState.ts';
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

  // Other state (could also be extracted if needed)
  const [permissions] = useState({ canEdit: true, canDelete: true, canMove: true });
  const [isAutoSaveEnabled] = useState(false);

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
      externalAppController
    };
  }, [form, mode, externalAppController, controller]);

  // Load initial form data
  useEffect(() => {
    loadForm();
  }, [permId, controller]);

  const loadForm = useCallback(async () => {
    console.log('loadForm', { permId }, { entityKind }, { params });
    
    const loadedForm = await controller.load(permId, entityKind, params);
    setForm(loadedForm);
  }, [permId, entityKind, params, controller, setForm]);

  // Early returns
  if (!form) {
    return null;
  }

  return (
    <>
      <EntityForm
        form={form}
        mode={mode}
        permissions={permissions}
        onFieldChange={updateField}
        onFieldMetadataChange={updateFieldMetadata}
        onAction={() => {}} // TODO: handleAction
        params={{ sessionID: sessionID }}
      />
    </>
  );
};

