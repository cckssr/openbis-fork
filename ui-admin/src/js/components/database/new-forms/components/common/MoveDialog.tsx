import React, { useState, useEffect } from 'react';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import Message from '@src/js/components/common/form/Message.jsx';
import TextField from '@src/js/components/common/form/TextField.jsx';
import { Form, FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { MoveService, MoveEntityConfig } from '@src/js/components/database/new-forms/services/MoveService.ts';
import { AdvancedEntitySearchDropdown } from '@src/js/components/database/new-forms/components/common/AdvancedEntitySearchDropdown.tsx';
import messages from '@src/js/common/messages';
import {
  Box,
  Typography,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Checkbox,
  FormGroup,
  FormHelperText,
  Divider
} from '@mui/material';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { getProjectIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';

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
  const [moveService, setMoveService] = useState<MoveService | null>(null);
  const [selectedTarget, setSelectedTarget] = useState<any>(null);
  const [moveDescendants, setMoveDescendants] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Initialize MoveService when dialog opens
  useEffect(() => {
    if (open && form && openbisFacade) {
      /* const config: MoveEntityConfig = {
        entityType: form.entityType as EntityKind,
        entityPermIds: form.entityPermId,
        openbisFacade,
        entityFormController,
        optionalPostAction: () => {
          console.log('Move operation completed');
        }
      };
      
      const service = new MoveService(config);
      setMoveService(service);
      
      // Initialize the service
      service.init().catch(err => {
        setError(`Failed to initialize move service: ${err.message}`);
      }); */
    }
  }, [open, form, openbisFacade, entityFormController]);

  const handleConfirm = async () => {
    /* if (!moveService) {
      setError('Move service not initialized');
      return;
    } */

    setLoading(true);
    setError(null);
 
    //try {
      // Set the selected target in the service
      //moveService.selected = selectedTarget;

      // Execute the move operation
      //const result = await moveService.move(moveDescendants);
      console.log('handleConfirm', selectedTarget, moveDescendants);
      const result = await entityFormController?.move(form, null, { target: selectedTarget, moveDescendants: moveDescendants });
      onConfirm(result);
    /* } catch (err: any) {
      setError(err.message || 'Move operation failed');
    } finally {
      setLoading(false);
    } */
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

  const currentParent = () => {
    switch (form.entityKind) {
      case EntityKind.PROJECT:
        return 'Space';
      case EntityKind.EXPERIMENT:
        return 'Space or Project';
      case EntityKind.SAMPLE:
        return 'Space or Project or Experiment/Collection';
      case EntityKind.DATASET:
        return 'Space or Project or Experiment/Collection or Sample';
      default:
        return 'Unknown';
    }
  };

  const renderEntityInfo = () => {
    if (!form) return null;
    
      const entity = moveInfo.entity;
      const identifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
      const type = form.entityType;
      const space = findFormFieldById(form.fields, form.entityPermId, 'space', true);
      const project = findFormFieldById(form.fields, form.entityPermId, 'project', true);
      //const owningEntityType = form.entityKind;
      //const owningEntityIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
      const projectIdentifier = getProjectIdentifierFromSampleIdentifier(identifier);

      return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 2 }}>
            {/* <Typography variant="body2">
              <strong>Type:</strong> {type}
            </Typography> 
            <Typography variant="body2">
              <strong>Current {project ? 'Project' : 'Space'}:</strong> {project ? projectIdentifier : space}
            </Typography>*/}
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
              label={`Current ${project ? 'Project' : 'Space'}:`}
              mode={FormMode.VIEW}
              disabled={true}
              value={project ? projectIdentifier : space}
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
    const identifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
    return `Moving ${identifier}`;
  };

  if (!moveInfo) {
    return (
      <Dialog
        open={open}
        onClose={handleCancel}
        title={getMoveTitle()}
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