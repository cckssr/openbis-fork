import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms-v2/core/types/index.ts';
import { DateFieldRenderer } from '@src/js/components/database/new-forms/components/fields/DateFieldRenderer.tsx';
import { FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

export const DateFieldAdapter: React.FC<FieldRendererProps> = ({ field, value, onChange, error, context }) => {
  // Convert our field structure to the expected structure for DateFieldRenderer
  const adaptedField = {
    ...field,
    value: value,
    required: field.required,
    readOnly: field.readOnly,
  };

  // Convert our context mode to the expected mode format
  const mode = context.mode === 'VIEW' ? 'view' : 
               context.mode === 'EDIT' ? 'edit' : 
               context.mode === 'CREATE' ? 'create' : 'view';

  // Convert our onChange to onFieldChange
  const onFieldChange = (fieldId: string, newValue: any) => {
    onChange(fieldId, newValue);
  };

  return (
    <DateFieldRenderer
      field={adaptedField}
      onFieldChange={onFieldChange}
      mode={mode as FormMode}
    />
  );
};
