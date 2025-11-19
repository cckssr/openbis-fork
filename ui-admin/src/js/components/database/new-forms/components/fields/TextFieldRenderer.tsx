import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import TextField from '@src/js/components/common/form/TextField.jsx';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

export const TextFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
  return (
    <TextField mandatory={field.required}
      label={field.label}
      mode={isEditing && !field.readOnly ? FormMode.EDIT : FormMode.VIEW}
      disabled={isEditing && field.readOnly}
      value={field.value}
      onChange={(e: React.ChangeEvent<HTMLInputElement>) => onFieldChange(field.id, e.target.value)}
      disableUnderline={true}
    />
  );
};