import React, { useState } from 'react';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import Message from '@src/js/components/common/form/Message.jsx';
import TextField from '@src/js/components/common/form/TextField.jsx';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { AdvancedEntitySearchDropdown } from '@src/js/components/database/new-forms/components/common/AdvancedEntitySearchDropdown.tsx';
import messages from '@src/js/common/messages';
import {
  Box,
  Typography,
  FormControlLabel,
  Checkbox,
  FormGroup
} from '@mui/material';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';

interface MoveDialogProps {
  open: boolean;
  onConfirm: (moveResult: any) => void;
  onCancel: () => void;
  form: Form;
  moveInfo: any;
  openbisFacade?: any;
  entityFormController?: IFormController;
}

const MoveDialog: React.FC<MoveDialogProps> = ({
  open,
  onConfirm,
  onCancel,
  form,
  moveInfo,
  openbisFacade,
  entityFormController
}) => {
  const [selectedTarget, setSelectedTarget] = useState<any>(null);
  const [moveDescendants, setMoveDescendants] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);


  const handleConfirm = async () => {
    setLoading(true);
    setError(null);
    const result = await entityFormController?.move(form, null, { target: selectedTarget, moveDescendants: moveDescendants });
    onConfirm(result);
  };

  const handleCancel = () => {
    setSelectedTarget(null);
    setMoveDescendants(false);
    setError(null);
    onCancel();
  };

  const handleTargetSelection = (target: any) => {
    setSelectedTarget(target);
    setError(null);
  };

  const renderEntityInfo = () => {
    if (!form) return null;

    const entityKind = moveInfo.entityKind;
    let identifier: string | null = null;
    let label: string | null = null;
    let space: string | null = null;
    let project: string | null = null;
    let collection: string | null = null;
    let object: string | null = null;
    let value: string | null = null;
    switch (entityKind) {
      case EntityKind.PROJECT:
        identifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true) as string | null;
        space = findFormFieldById(form.fields, form.entityPermId, 'space', true) as string | null;
        label = 'Current Space:';
        value = space;
        break;
      case EntityKind.SAMPLE:
      case EntityKind.OBJECT:
        identifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true) as string | null;
        space = findFormFieldById(form.fields, form.entityPermId, 'space', true) as string | null;
        project = findFormFieldById(form.fields, form.entityPermId, 'project', true) as string | null;
        collection = findFormFieldById(form.fields, form.entityPermId, 'collection', true) as string | null;
        label = `Current ${collection ? 'Collection' : project ? 'Project' : space ? 'Space' : 'Unknown'}:`;
        value = collection ? collection : project ? project : space ? space : null;
        break;
      case EntityKind.COLLECTION:
      case EntityKind.EXPERIMENT:
        identifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true) as string | null;
        project = findFormFieldById(form.fields, form.entityPermId, 'project', true) as string | null;
        label = 'Current Project:';
        value = project;
        break;
      case EntityKind.DATASET:
        object = findFormFieldById(form.fields, form.entityPermId, 'object', true) as string | null;
        collection = findFormFieldById(form.fields, form.entityPermId, 'collection', true) as string | null;
        label = `Current ${collection ? 'Collection' : object ? 'Object' : 'Unknown'}:`;
        value = collection ? collection : object ? object : null;
        break;
      default:
        return null;
    }
    const type = form.entityType;

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 2 }}>
        <TextField
          mandatory={true}
          label='Type'
          mode={FormMode.VIEW}
          disabled={true}
          value={type}
          disableUnderline={true}
        />
        <TextField
          mandatory={true}
          label={label}
          mode={FormMode.VIEW}
          disabled={true}
          value={value}
          disableUnderline={true}
        />
      </Box>
    );
  };

  const renderDescendantsOption = () => {
    if (form.entityKind !== EntityKind.SAMPLE) return null;

    return (
      <Box sx={{ mb: 2 }}>
        <FormGroup>
          <FormControlLabel
            control={
              <Checkbox
                checked={moveDescendants}
                onChange={(e) => setMoveDescendants(e.target.checked)}
              />
            }
            label={
              <Typography variant="body2" fontWeight='bold'>
                Click the checkbox if also all descendant objects (i.e. children, grand children etc.) including their data sets should be moved.
                Only those descendants are moved which belong to the same entity as this object.
              </Typography>
            }
          />
        </FormGroup>
      </Box>
    );
  };

  const renderEntitySelection = () => {
    return (
      <Box sx={{ mt: 2 }}>
        <AdvancedEntitySearchDropdown
          openbisFacade={openbisFacade}
          entityType={form.entityKind}
          onSelectionChange={handleTargetSelection}
          selectedEntity={selectedTarget}
        />
      </Box>
    );
  };

  const getMoveTitle = () => {
    if (!form) return 'Move Entity';
    const entityKind = moveInfo.entityKind;
    const identifier = findFormFieldById(form.fields, form.entityPermId, entityKind === EntityKind.DATASET ? 'object' : 'identifier', true);
    return `Moving ${identifier}`;
  };

  if (!moveInfo) {
    return (
      <Dialog
        open={open}
        onClose={handleCancel}
        title="No move info"
        content={
          <Box>
            <Message type="error">No move information available</Message>
          </Box>
        }
        actions={
          <Button label="Close" onClick={handleCancel} />
        }
      />
    );
  }

  return (
    <Dialog
      open={open}
      onClose={handleCancel}
      title={getMoveTitle()}
      content={
        <>
          {error && (
            <Box sx={{ mb: 2 }}>
              <Message type="error">{error}</Message>
            </Box>
          )}

          {renderEntityInfo()}
          {/* renderSampleOptions() */}
          {renderDescendantsOption()}
          {renderEntitySelection()}
        </>
      }
      actions={
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
          <Button
            label={messages.get(messages.CANCEL)}
            onClick={handleCancel}
            disabled={loading}
          />
          <Button
            label={messages.get(messages.CONFIRM)}
            type='final'
            onClick={handleConfirm}
            disabled={loading || !selectedTarget}
          />
        </Box>
      }
    />
  );
};

export default MoveDialog; 