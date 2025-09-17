import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms-v2/core/types/index.ts';
import { TextAreaFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextAreaFieldRenderer.tsx';
import { FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

export const TextAreaFieldAdapter: React.FC<FieldRendererProps> = ({ field, value, onChange, error, context }) => {
  // Convert our field structure to the expected structure for TextAreaFieldRenderer
  const adaptedField = {
    ...field,
    value: value,
    required: field.required || false,
    readOnly: field.readOnly || false,
    isMultiValue: false,
    section: 'GENERAL' as any,
    dataType: field.dataType || 'MULTILINE_VARCHAR' as any,
    meta: field.meta || {},
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
    <TextAreaFieldRenderer
      field={adaptedField}
      onFieldChange={onFieldChange}
      mode={mode as FormMode}
    />
  );
};
