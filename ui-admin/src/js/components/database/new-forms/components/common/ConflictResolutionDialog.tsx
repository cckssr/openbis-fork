import React, { useState } from 'react';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import Message from '@src/js/components/common/form/Message.jsx';
import { FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import TextField from '@src/js/components/common/form/TextField.jsx';
import RadioGroup from '@mui/material/RadioGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Radio from '@mui/material/Radio';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import messages from '@src/js/common/messages';

interface ConflictResolutionDialogProps {
  open: boolean;
  conflicts: [FormField, FormField][];
  onResolve: (resolved: Record<string, any>) => void;
  onCancel: () => void;
}

const ConflictResolutionDialog: React.FC<ConflictResolutionDialogProps> = ({ open, conflicts, onResolve, onCancel }) => {
  const [choices, setChoices] = useState<Record<string, 'local' | 'server' | 'merge'>>({});
  const [mergedValues, setMergedValues] = useState<Record<string, any>>({});

  const handleChoice = (id: string, choice: 'local' | 'server' | 'merge', localValue: any, serverValue: any) => {
    setChoices(prev => ({ ...prev, [id]: choice }));
    if (choice === 'merge' && mergedValues[id] === undefined) {
      // Default merge: join as string
      setMergedValues(prev => ({ ...prev, [id]: `${localValue} \n ${serverValue}` }));
    }
  };

  const handleMergeEdit = (id: string, value: any) => {
    setMergedValues(prev => ({ ...prev, [id]: value }));
    setChoices(prev => ({ ...prev, [id]: 'merge' }));
  };

  const handleConfirm = () => {
    const resolved: Record<string, any> = {};
    for (const [localField, serverField] of conflicts) {
      const choice = choices[localField.id] || 'local';
      if (choice === 'local') {
        resolved[localField.id] = localField.value;
      } else if (choice === 'server') {
        resolved[localField.id] = serverField.value;
      } else if (choice === 'merge') {
        resolved[localField.id] = mergedValues[localField.id];
      }
    }
    onResolve(resolved);
  };

  const allChosen = conflicts.length > 0 && conflicts.every(([localField]) => choices[localField.id]);

  return (
    <Dialog
      open={open}
      onClose={onCancel}
      title={'Resolve Conflicts'}
      content={
        <div>
          <Message type='warning'>Some fields have conflicting changes. Choose which value to keep for each field.</Message>
          {conflicts.map(([localField, serverField]) => (
            <div key={localField.id} style={{ margin: '16px 0', padding: 8, border: '1px solid #eee', borderRadius: 4 }}>
              <div><b>{localField.label}</b></div>
              <RadioGroup
                row
                name={localField.id}
                value={choices[localField.id] || 'local'}
                onChange={(_, value) => handleChoice(localField.id, value as 'local' | 'server' | 'merge', localField.value, serverField.value)}
              >
                <FormControlLabel
                  value="local"
                  control={<Radio />}
                  label={
                    <TextField
                      mandatory={localField.required}
                      label={localField.label + ' (Local)'}
                      mode={FormMode.VIEW}
                      disabled={true}
                      value={localField.value}
                      disableUnderline={true}
                    />
                  }
                />
                <FormControlLabel
                  value="server"
                  control={<Radio />}
                  label={
                    <TextField
                      mandatory={serverField.required}
                      label={serverField.label + ' (Server)'}
                      mode={FormMode.VIEW}
                      disabled={true}
                      value={serverField.value}
                      disableUnderline={true}
                    />
                  }
                />
                <FormControlLabel
                  value="merge"
                  control={<Radio />}
                  label={
                    <TextField
                      mandatory={localField.required}
                      label={localField.label + ' (Merge)'}
                      mode={choices[localField.id] === 'merge' ? FormMode.EDIT : FormMode.VIEW}
                      disabled={choices[localField.id] !== 'merge'}
                      value={choices[localField.id] === 'merge' ? mergedValues[localField.id] ?? '' : mergedValues[localField.id] ?? ''}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleMergeEdit(localField.id, e.target.value)}
                      disableUnderline={true}
                    />
                  }
                />
              </RadioGroup>
            </div>
          ))}
        </div>
      }
      actions={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button label={messages.get(messages.CANCEL)} onClick={onCancel} />
          <Button label={messages.get(messages.CONFIRM)} onClick={handleConfirm} disabled={!allChosen} />
        </div>
      }
    />
  );
};

export default ConflictResolutionDialog; 