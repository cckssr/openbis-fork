import React from 'react';
import { ActionRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormControlLabel, FormGroup, Switch } from '@mui/material';

export const SwitchActionRenderer: React.FC<ActionRendererProps> = ({ action, onAction, mode }) => {

  return (
    <FormGroup>
      <FormControlLabel key={action.name}
        name={action.name}
        control={<Switch size='small' checked={action.value} onChange={() => action.handler?.(action.name)} color='primary' />}
        label={action.label}
        labelPlacement='start'
      />
    </FormGroup>
  );
};
