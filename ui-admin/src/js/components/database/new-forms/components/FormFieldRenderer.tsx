import React from 'react';
import { FormField, FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import TextField from '@src/js/components/common/form/TextField.jsx';
import DateField from '@src/js/components/common/form/DateField.jsx';

import { FormControl, TextareaAutosize, Typography } from '@mui/material';

interface FormFieldRendererProps {
  field: FormField;
  onUpdate: (fieldId: string, value: any) => void;
  isEditing: boolean;
  mode: FormMode;
}

export const FormFieldRenderer: React.FC<FormFieldRendererProps> = ({ field, onUpdate, isEditing, mode }) => {
  const { id, label, value, dataType, meta, isMandatory, isEditable } = field;

  // If not editing, just display the value
  if (!isEditing) {
    return <TextField
      mandatory={isMandatory}
      label={label}
      mode='view'
      variant='standard'
      value={value?.toString() || '–'} />

  }

  // Render the correct input based on dataType
  const renderInput = () => {
    switch (dataType) {
      case FormFieldDataType.VARCHAR:
        return <TextField
          mandatory={isMandatory}
          label={label}
          mode={isEditing ? 'edit' : 'view'}
          disabled={isEditing && !isEditable}
          value={value}
          onChange={e => onUpdate(id, e.target.value)} />;
      case FormFieldDataType.MULTILINE_VARCHAR:
        return (<FormControl>
          <Typography variant="body2" component={'span'} sx={{ color: "textSecondary" }}>
            {label}
          </Typography>
          <TextareaAutosize name='text-area-comment'
            placeholder="Add a comment"
            value={value}
            onChange={event => onUpdate(id, event.target.value)} />
        </FormControl>)
      case FormFieldDataType.INTEGER:
        return <input type="number" value={value} onChange={e => onUpdate(id, parseInt(e.target.value, 10))} />;
      case FormFieldDataType.BOOLEAN:
        return <input type="checkbox" checked={!!value} onChange={e => onUpdate(id, e.target.checked)} />;
      case FormFieldDataType.TIMESTAMP:
        console.log(value);
        return <DateField
          label={label}
          name={label + '-Date'}
          mandatory={isMandatory}
          disabled={isEditing && !isEditable}
          value={value}
          mode={mode}
        />
      case FormFieldDataType.CONTROLLED_VOCABULARY:
        return (
          <select value={value} onChange={e => onUpdate(id, e.target.value)}>
            {meta.vocabularyOptions?.map(opt => <option key={opt.code} value={opt.code}>{opt.label}</option>)}
          </select>
        );
      case FormFieldDataType.WORD_PROCESSOR:
        // return <RichTextEditor content={value} onChange={content => onUpdate(id, content)} />;
        return <div>Word Processor for {label}</div>;
      default:
        return <input type="text" value={value} disabled title={`Unsupported type: ${dataType}`} />;
    }
  };

  return (
    <div className="form-field-edit">
      {renderInput()}
      {meta.helpText && <span className="tooltip">{meta.helpText}</span>}
    </div>
  );
};