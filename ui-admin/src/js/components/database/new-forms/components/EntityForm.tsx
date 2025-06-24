import React, { useState, useEffect, useCallback } from 'react';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.tsx';
import { FormFieldRenderer } from '@src/js/components/database/new-forms/components/FormFieldRenderer.tsx';
import { Toolbar, Switch, Dialog, FormGroup, FormControlLabel, Alert, Snackbar, SnackbarCloseReason, Stack, Grid2, Accordion, AccordionSummary, AccordionDetails, Typography } from '@mui/material'
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx';
import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';
import messages from '@src/js/common/messages.js';
import Button from '@src/js/components/common/form/Button.jsx';
import Container from '@src/js/components/common/form/Container.jsx'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';

import { makeStyles } from '@mui/styles';
import Message from '@src/js/components/common/form/Message.jsx';

interface EntityFormProps {
  initialForm: Form;
  initialMode: FormMode;
  controller: FormController;
  customToolbar: any;
  customSections: any;
}

export const EntityForm: React.FC<EntityFormProps> = ({ initialForm, initialMode, controller, customToolbar, customSections }) => {
  const [form, setForm] = useState<Form>(initialForm);
  const [mode, setMode] = useState<FormMode>(initialMode);
  const [permissions, setPermissions] = useState({ canEdit: true, canDelete: true, canMove: true });
  const [isAutoSaveEnabled, setAutoSaveEnabled] = useState(false);
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  const { isConflicted, resolveConflicts } = useConflictResolution();

  // Callback to update a single field's value
  const handleFieldUpdate = useCallback((fieldId: string, value: any) => {
    setForm(prevForm => ({
      ...prevForm,
      fields: prevForm.fields.map(field =>
        field.id === fieldId ? { ...field, value } : field
      ),
    }));
  }, []);

  const handleSave = useCallback(async () => {
    try {
      const newVersion = await controller.save(form);
      console.log({ newVersion })
      setForm(prev => ({ ...prev, version: newVersion }));
      setMode(FormMode.VIEW);
      setShowSuccess(true);
    } catch (error: any) {
      if (error.status === 409) {
        const latestForm = await controller.load(form.entityPermId);
        resolveConflicts(form, latestForm);
      } else {
        console.error("Save failed:", error);
      }
    }
  }, [controller, form, resolveConflicts]);

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
    console.log("Delete action initiated.");
    setDeleteModalOpen(true);
  };

  const handleMove = () => {
    alert("Move functionality not yet implemented.");
  };

  useAutoSave({
    formData: form,
    storageKey: `entity-form-${form.entityPermId}`,
    isEnabled: isAutoSaveEnabled && mode === FormMode.EDIT,
    interval: 60000,
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
      return customToolbar({ form, mode, controller })
    else
      return (
        <Stack direction='row' spacing={{ xs: 1, sm: 2 }} sx={{ mb: 2 }}>
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
            type='risky'
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
          <FormGroup>
            <FormControlLabel
              name='autosave-control-switch'
              control={<Switch size='small' checked={isAutoSaveEnabled} onChange={event => setAutoSaveEnabled(prev => !prev)} color='primary' />}
              label='Auto-Save'
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
        {Object.entries(fieldsBySection).map(([sectionName, fields]) => (
          <CollapsableSection isCollapsed={true} title={sectionName} renderWarnings={renderWarningChanges(mode != FormMode.EDIT)}>
            <Grid2 container spacing={2}>
              {fields.map(field => (
                <Grid2 size={{ xs:12, sm: 6, md:4 }} key={field.id}>
                  <FormFieldRenderer
                    field={field}
                    onUpdate={handleFieldUpdate}
                    isEditing={mode === FormMode.EDIT}
                    mode={mode}
                  />
                </Grid2>
              ))}
            </Grid2>
          </CollapsableSection>
        ))}
      </>
    );
  };

  return (
    <div className="entity-form-container">
      <Snackbar onClose={handleClose} open={showSuccess} autoHideDuration={2000} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" sx={{ width: '100%' }}>
          Space updated
        </Alert>
      </Snackbar>
      {isConflicted && (
        <div className="conflict-banner">
          This item has been modified by someone else. Please review the changes before saving.
        </div>
      )}

      {renderToolbar()}


      <div className="form-body">
        <div className="form-sections">
          {renderFieldsBySection()}
        </div>

        {form.entityKind === 'SAMPLE' && (
          <div className="related-samples-section">
            {/* <h2>Parents/Children</h2>
              <YourExistingTableWidget entityPermId={form.entityPermId} /> */}
          </div>
        )}

        {form.entityKind === 'DATASET' && (
          <div className="dataset-upload-section">
            {/* <h2>File Upload (AFS)</h2>
              <YourAdvancedFileUploadWidget entityPermId={form.entityPermId} /> */}
          </div>
        )}
      </div>

      <Dialog
        open={isDeleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onBackdropClick={() => console.log("Deletion confirmed!")}
      />
    </div>
  );
};