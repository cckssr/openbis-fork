import React, { useState, useEffect, useCallback } from 'react';
import { EntityKind, Form, FormAction, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/entities/FormController.tsx';
import { FormFieldRenderer } from '@src/js/components/database/new-forms/components/FormFieldRenderer.tsx';
import { Toolbar, Switch, Dialog, FormGroup, FormControlLabel, Alert, Snackbar, SnackbarCloseReason, Stack, Grid2, Accordion, AccordionSummary, AccordionDetails, Typography } from '@mui/material'
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx';
import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';
import messages from '@src/js/common/messages.js';
import Button from '@src/js/components/common/form/Button.jsx';
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContextProvider.tsx';

import Message from '@src/js/components/common/form/Message.jsx';
import ConflictResolutionDialog from '@src/js/components/database/new-forms/components/ConflictResolutionDialog.tsx';
import { groupFieldsBySection, SectionGroup } from '@src/js/components/database/new-forms/adapters/sectionBuilder.ts';

interface EntityFormProps {
  initialForm: Form;
  initialMode: FormMode;
  controller: FormController;
  customToolbar: any;
  customSections: any;
  onAfterSave?: () => void;
  actions?: FormAction[];
}

export const EntityForm: React.FC<EntityFormProps> = ({ initialForm, initialMode, controller, customToolbar, customSections, onAfterSave, actions }) => {
  const { form, setForm, mode, setMode } = useEntityForm();
  const [permissions, setPermissions] = useState({ canEdit: true, canDelete: true, canMove: true });
  const [isAutoSaveEnabled, setAutoSaveEnabled] = useState(false);
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [conflictWarning, setConflictWarning] = useState<string | null>(null);
  const [showConflictDialog, setShowConflictDialog] = useState(false);
  const [conflictFields, setConflictFields] = useState<any[]>([]);
  const [conflictResolutionActive, setConflictResolutionActive] = useState(false);

  const { isConflicted, conflictingFields, resolveConflicts, checkModificationDateConflict, findConflicts } = useConflictResolution();
  const { onNewProject, onEntityChange } = useEntityForm();

  const handleModeChange = (mode: FormMode) => {
    setMode(mode);
  }
  // Callback to update a single field's value
  const handleFieldUpdate = useCallback((fieldId: string, value: any) => {
    setForm(prevForm => ({ ...prevForm, fields: prevForm.fields.map(field => field.id === fieldId ? { ...field, value } : field) }));
    if (onEntityChange) onEntityChange(form.entityPermId, true);
  }, []);

  const validateForm = (form: Form): string | null => {
    for (const field of form.fields) {
      if (field.required && (field.value === undefined || field.value === null || field.value === '')) {
        return `Field "${field.label}" is mandatory.`;
      }
    }
    return null;
  };

  const handleSave = useCallback(async () => {
    setValidationError(null);
    setConflictWarning(null);
    // 1. Validate mandatory fields
    const validationMsg = validateForm(form);
    if (validationMsg) {
      setValidationError(validationMsg);
      return;
    }
    // 2. Check for conflicts
    try {
      const latestForm = await controller.load(form.entityPermId);
      if (!conflictResolutionActive && checkModificationDateConflict(form, latestForm)) {
        // Use resolveConflicts to get merged fields with conflict info
        const conflicts = findConflicts(form, latestForm) as any[];

        setConflictFields(conflicts);
        setShowConflictDialog(true);
        return;
      }
      // No conflicts, proceed to save
      const newVersion = await controller.save(form);
      setForm(prev => ({ ...prev, version: newVersion }));
      handleModeChange(FormMode.VIEW);
      setShowSuccess(true);
      setConflictResolutionActive(false);
      if (onEntityChange) onEntityChange(form.entityPermId, false);
      if (onAfterSave) onAfterSave();
    } catch (error: any) {
      if (error.status === 409) {
        const latestForm = await controller.load(form.entityPermId);
        resolveConflicts(form, latestForm);
      } else {
        console.error('Save failed:', error);
      }
    }
  }, [controller, form, resolveConflicts, onAfterSave, checkModificationDateConflict, conflictResolutionActive, findConflicts]);

  const handleResolveConflicts = (resolved: Record<string, any>) => {
    setForm(prevForm => ({
      ...prevForm,
      fields: prevForm.fields.map(field =>
        resolved.hasOwnProperty(field.id) ? { ...field, value: resolved[field.id] } : field
      ),
    }));
    setShowConflictDialog(false);
    handleModeChange(FormMode.EDIT)
    setConflictResolutionActive(true);
  };

  const handleEdit = () => {
    handleModeChange(FormMode.EDIT);
  };

  const handleClose = (
    event: React.SyntheticEvent | Event,
    reason?: SnackbarCloseReason,
  ) => {
    if (reason === 'clickaway') {
      return;
    }
    setShowSuccess(false);
  };

  const handleDelete = () => {
    console.log('Delete action initiated.');
    setDeleteModalOpen(true);
  };

  const handleMove = () => {
    alert('Move functionality not yet implemented.');
  };

  

  useEffect(() => {
    controller.checkPermissions(form).then(setPermissions);
  }, [controller, form]);


  const renderWarningChanges = (isSaved: boolean) => {
    return !isSaved && (<Message type='warning'>
      {messages.get(messages.UNSAVED_CHANGES)}
    </Message>
    )
  }

  const defaultActions = [
    {
      name: 'edit',
      label: 'Edit',
      component: 'button',
      handler: handleEdit,
      isAllowed: true,
      isVisible: true
    },
    {
      name: 'save',
      label: 'Save',
      component: 'button',
      handler: handleSave,
      isAllowed: true,
      isVisible: mode === FormMode.EDIT
    },
    {
      name: 'cancel',
      label: 'Cancel',
      component: 'button',
      handler: () => handleModeChange(FormMode.VIEW),
      isAllowed: true,
      isVisible: mode === FormMode.EDIT
    },
    {
      name: 'delete',
      label: 'Delete',
      component: 'button',
      handler: handleDelete,
      isAllowed: true,
      isVisible: true
    },
    {
      name: 'autosave',
      label: 'Keep-Draft',
      component: 'switch',
      handler: (event: React.ChangeEvent<HTMLInputElement>) => setAutoSaveEnabled(event.target.checked),
      isAllowed: true,
      isVisible: true,
      value: isAutoSaveEnabled
    }
  ]

  const renderAction = (action: FormAction) => {
    console.log({ action });
    if (action.component === 'button') {
      return (
        <Button
          id={action.name}
          label={action.label}
          type='neutral'
          onClick={action.handler}
          disabled={!action.isAllowed}
          hidden={!action.isVisible}
        />
      )
    } else if (action.component === 'switch') {
      return (<FormGroup>
        <FormControlLabel
          name={action.name}
          control={<Switch size='small' checked={action.value} onChange={action.handler} color='primary' />}
          label={action.label}
          labelPlacement='start'
          disabled={form.mode != FormMode.EDIT}
        />
      </FormGroup>
      )
    }
  }

  const renderActions = () => {
    console.log(form?.actions);

    return (
      <Stack key={form.entityPermId + 'actions'} direction='row' spacing={{ xs: 1, sm: 2 }} sx={{
        justifyContent: 'flex-start',
        alignItems: 'center',
        padding: '16px 16px',
        backgroundColor: 'rgb(248,248,248)'
      }}>
        {defaultActions.map(action => renderAction(action as FormAction))}
        {form?.actions && form?.actions.map((action: FormAction) => renderAction(action))}
      </Stack>
    )
  }

  const renderToolbar = () => {
    if (customToolbar)
      return customToolbar()
    else
      return (
        <Stack direction='row' spacing={{ xs: 1, sm: 2 }} sx={{
          justifyContent: 'flex-start',
          alignItems: 'center',
          padding: '16px 16px',
          backgroundColor: 'rgb(248,248,248)'
        }}>
          {form.mode === FormMode.VIEW && permissions.canEdit && <Button
            id='space-edit'
            label={messages.get(messages.EDIT)}
            type='neutral'
            onClick={handleEdit}
          />}
          {form.mode === FormMode.EDIT && <Button
            id='space-save'
            label={messages.get(messages.SAVE)}
            type='final'
            onClick={handleSave}
          />}
          {form.mode === FormMode.EDIT && <Button
            id='space-cancel'
            label={messages.get(messages.CANCEL)}
            type='neutral'
            styles={{ root: '' }}
            onClick={() => handleModeChange(FormMode.VIEW)}
          />}
          {permissions.canDelete && <Button
            id='space-delete'
            label={messages.get(messages.DELETE)}
            type='neutral'
            onClick={handleDelete}
            disabled={form.mode === FormMode.EDIT}
          />}
          {permissions.canMove && <Button
            id='space-move'
            label={messages.get(messages.MOVE)}
            type='neutral'
            onClick={handleMove}
            disabled={form.mode === FormMode.EDIT}
          />}
          <Button
            id='new=project'
            label={messages.get(messages.NEW) + ' ' + messages.get(messages.PROJECT)}
            type='neutral'
            onClick={() => onNewProject(form.entityPermId)}
            disabled={form.mode === FormMode.EDIT}
          />
          <FormGroup>
            <FormControlLabel
              name='autosave-control-switch'
              control={<Switch size='small' checked={isAutoSaveEnabled} onChange={event => setAutoSaveEnabled(prev => !prev)} color='primary' />}
              label='Keep-Draft'
              labelPlacement='start'
              disabled={form.mode != FormMode.EDIT}
            />
          </FormGroup>
        </Stack>)
  }

  // Modular section rendering
  const renderSections = () => {
    const sections: SectionGroup[] = groupFieldsBySection(form.fields);
    return (
      <>
        {sections.map(({ section, fields }) => {
          // Group fields by column within each section
          const leftFields = fields.filter(field => field.column === 'left');
          const rightFields = fields.filter(field => field.column === 'right');
          const centerFields = fields.filter(field => field.column === 'center');

          return (
            <CollapsableSection isCollapsed={false} title={section} renderWarnings={null} key={section}>
              <div style={{ padding: '8px 16px' }}>
                {/* Render left and right columns side by side */}
                {(leftFields.length > 0 || rightFields.length > 0) && (
                  <div style={{ display: 'flex', gap: '16px' }}>
                    {/* Left column */}
                    <div style={{ flex: 1 }}>
                      {leftFields.map(field => (
                        <div key={field.id} style={{ marginBottom: '8px' }}>
                          <FormFieldRenderer
                            field={field}
                            onUpdate={handleFieldUpdate}
                            isEditing={form.mode === FormMode.EDIT}
                            mode={form.mode}
                          />
                        </div>
                      ))}
                    </div>
                    {/* Right column */}
                    <div style={{ flex: 1 }}>
                      {rightFields.map(field => (
                        <div key={field.id} style={{ marginBottom: '8px' }}>
                          <FormFieldRenderer
                            field={field}
                            onUpdate={handleFieldUpdate}
                            isEditing={form.mode === FormMode.EDIT}
                            mode={form.mode}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {/* Render center fields on their own row */}
                {centerFields.length > 0 && (
                  <div>
                    {centerFields.map(field => (
                      <div key={field.id} style={{ marginBottom: '8px' }}>
                        <FormFieldRenderer
                          field={field}
                          onUpdate={handleFieldUpdate}
                          isEditing={form.mode === FormMode.EDIT}
                          mode={form.mode}
                        />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CollapsableSection>
          );
        })}
      </>
    );
  };

  return (
    <>
      {/* renderToolbar() */}
      {renderActions()}
      <div className='entity-form-container'>
        <Snackbar onClose={handleClose} open={showSuccess} autoHideDuration={2000} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity='success' sx={{ width: '100%' }}>
            Space updated
          </Alert>
        </Snackbar>
        {validationError && (
          <Alert severity='error' sx={{ width: '100%', marginBottom: 2 }}>{validationError}</Alert>
        )}
        {conflictWarning && (
          <Alert severity='warning' sx={{ width: '100%', marginBottom: 2 }}>{conflictWarning}</Alert>
        )}
        {isConflicted && (
          <div className='conflict-banner'>
            This item has been modified by someone else. Please review the changes before saving.
          </div>
        )}

        <div className='form-body'>
          <div className='form-sections'>
            {renderSections()}
          </div>

          {form.entityKind === 'SAMPLE' && (
            <div className='related-samples-section'>
              {/* <h2>Parents/Children</h2>
              <YourExistingTableWidget entityPermId={form.entityPermId} /> */}
            </div>
          )}

          {form.entityKind === 'DATASET' && (
            <div className='dataset-upload-section'>
              {/* <h2>File Upload (AFS)</h2>
              <YourAdvancedFileUploadWidget entityPermId={form.entityPermId} /> */}
            </div>
          )}
        </div>

        <Dialog
          open={isDeleteModalOpen}
          onClose={() => setDeleteModalOpen(false)}
          onBackdropClick={() => console.log('Deletion confirmed!')}
        />

        {showConflictDialog && (
          <ConflictResolutionDialog
            open={showConflictDialog}
            conflicts={conflictFields}
            onResolve={handleResolveConflicts}
            onCancel={() => setShowConflictDialog(false)}
          />
        )}
      </div>
    </>
  );
};