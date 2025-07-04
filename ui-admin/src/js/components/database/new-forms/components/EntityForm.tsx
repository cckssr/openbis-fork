import React, { useState, useEffect, useCallback } from 'react';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.tsx';
import { FormFieldRenderer } from '@src/js/components/database/new-forms/components/FormFieldRenderer.tsx';
import { Toolbar, Switch, Dialog, FormGroup, FormControlLabel, Alert, Snackbar, SnackbarCloseReason, Stack, Grid2, Accordion, AccordionSummary, AccordionDetails, Typography } from '@mui/material'
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx';
import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';
import messages from '@src/js/common/messages.js';
import Button from '@src/js/components/common/form/Button.jsx';
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';

import Message from '@src/js/components/common/form/Message.jsx';
import Container from '@src/js/components/common/form/Container.jsx';
import ConflictResolutionDialog from './ConflictResolutionDialog.tsx';

interface EntityFormProps {
  initialForm: Form;
  initialMode: FormMode;
  controller: FormController;
  customToolbar: any;
  customSections: any;
  onAfterSave?: () => void;
  onEntityChange?: (permId: string, changed: boolean) => void;
  onNewProject?: () => void;
}

export const EntityForm: React.FC<EntityFormProps> = ({ initialForm, initialMode, controller, customToolbar, customSections, onAfterSave, onEntityChange, onNewProject }) => {
  const [form, setForm] = useState<Form>(initialForm);
  const [mode, setMode] = useState<FormMode>(initialMode);
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

  // Callback to update a single field's value
  const handleFieldUpdate = useCallback((fieldId: string, value: any) => {
    setForm(prevForm => ({
      ...prevForm,
      fields: prevForm.fields.map(field =>
        field.id === fieldId ? { ...field, value } : field
      ),
    }));
    if (onEntityChange) onEntityChange(form.entityPermId, true);
  }, []);

  const validateForm = (form: Form): string | null => {
    for (const field of form.fields) {
      if (field.isMandatory && (field.value === undefined || field.value === null || field.value === '')) {
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
      setMode(FormMode.VIEW);
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
    setMode(FormMode.EDIT); // Stay in edit mode
    setConflictResolutionActive(true);
  };

  const handleEdit = () => {
    setMode(FormMode.EDIT);
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

  useAutoSave({
    formData: form,
    storageKey: `entity-form-${form.entityPermId}`,
    isEnabled: isAutoSaveEnabled && mode === FormMode.EDIT,
    interval: 15000,
    onDataRestore: (restoredForm) => console.log(restoredForm) //setForm(restoredForm),
  });

  useEffect(() => {
    controller.checkPermissions(form).then(setPermissions);
  }, [controller, form]);


  const renderWarningChanges = (isSaved: boolean) => {
    return !isSaved && (<Message type='warning'>
      {messages.get(messages.UNSAVED_CHANGES)}
    </Message>
    )
  }

  const renderToolbar = () => {
    if (customToolbar)
      return customToolbar()
    else
      return (
        <Stack direction='row' spacing={{ xs: 1, sm: 2 }} sx={{ justifyContent: 'flex-start', 
          alignItems: 'center', 
          padding: '16px 16px', 
          backgroundColor: 'rgb(248,248,248)'}}>
          {mode === FormMode.VIEW && permissions.canEdit && <Button
            id='space-edit'
            label={messages.get(messages.EDIT)}
            type='neutral'
            onClick={handleEdit}
          />}
          {mode === FormMode.EDIT && <Button
            id='space-save'
            label={messages.get(messages.SAVE)}
            type='final'
            onClick={handleSave}
          />}
          {mode === FormMode.EDIT && <Button
            id='space-cancel'
            label={messages.get(messages.CANCEL)}
            type='neutral'
            styles={{ root: '' }}
            onClick={() => setMode(FormMode.VIEW)}
          />}
          {permissions.canDelete && <Button
            id='space-delete'
            label={messages.get(messages.DELETE)}
            type='neutral'
            onClick={handleDelete}
            disabled={mode === FormMode.EDIT}
          />}
          {permissions.canMove && <Button
            id='space-move'
            label={messages.get(messages.MOVE)}
            type='neutral'
            onClick={handleMove}
            disabled={mode === FormMode.EDIT}
          />}
          <Button
            id='new=project'
            label={messages.get(messages.NEW)}
            type='neutral'
            onClick={onNewProject}
            disabled={mode === FormMode.EDIT}
          />
          <FormGroup>
            <FormControlLabel
              name='autosave-control-switch'
              control={<Switch size='small' checked={isAutoSaveEnabled} onChange={event => setAutoSaveEnabled(prev => !prev)} color='primary' />}
              label='Keep-Draft'
              labelPlacement='start'
              disabled={mode != FormMode.EDIT}
            />
          </FormGroup>
        </Stack>)
  }

  const renderFieldsBySection = () => {
    // Group fields by section
    const fieldsBySection = form.fields.reduce((acc: { [key: string]: any[] }, field) => {
      const sectionName = field.section || 'Custom Section';
      if (!acc[sectionName]) {
        acc[sectionName] = [];
      }
      acc[sectionName].push(field);
      return acc;
    }, {});

    return (
      <>
        {Object.entries(fieldsBySection).map(([sectionName, fields]) => {
          // @ts-ignore
          return (
            <CollapsableSection isCollapsed={true} title={sectionName} renderWarnings={null}>
              <Container>
                <Grid2 container spacing={2}>
                  {fields.map(field => (
                    <Grid2 size={{ xs:12, sm: 6 }} key={field.id}>
                      <FormFieldRenderer
                        field={field}
                        onUpdate={handleFieldUpdate}
                        isEditing={mode === FormMode.EDIT}
                        mode={mode}
                      />
                    </Grid2>
                  ))}
                </Grid2>
              </Container>
            </CollapsableSection>
          );
        })}
      </>
    );
  };

  return (
    <>
    {renderToolbar()}
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
          {renderFieldsBySection()}
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