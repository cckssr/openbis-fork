import React from 'react';
import { FormField, FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import TextField from '@src/js/components/common/form/TextField.jsx';
import DateField from '@src/js/components/common/form/DateField.jsx';
import TextAreaField from '@src/js/components/common/form/TextAreaField.jsx';

interface FormFieldRendererProps {
  field: FormField;
  onUpdate: (fieldId: string, value: any) => void;
  isEditing: boolean;
  mode: FormMode;
}

export const FormFieldRenderer: React.FC<FormFieldRendererProps> = ({ field, onUpdate, isEditing, mode }) => {
  const { id, label, value, dataType, meta, required, readOnly } = field;

  const renderInput = () => {
    switch (dataType) {
      case FormFieldDataType.VARCHAR:
        return (<TextField
          mandatory={required}
          label={label}
          mode={isEditing && !readOnly ? 'edit' : 'view'}
          disabled={isEditing && readOnly}
          value={value}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate(id, e.target.value)}
          disableUnderline={true}
        />)
      case FormFieldDataType.MULTILINE_VARCHAR:
        return (<TextAreaField id={id}
          name={label}
          label={label}
          value={value}
          mandatory={required}
          disabled={isEditing && readOnly}
          mode={isEditing && !readOnly ? 'edit' : 'view'}
          onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) => onUpdate(id, event.target.value)}
          description={meta?.helpText}
          disableUnderline={true}
        />
        );
      case FormFieldDataType.INTEGER:
        return <input type="number" value={value} onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate(id, parseInt(e.target.value, 10))} />;
      case FormFieldDataType.BOOLEAN:
        return <input type="checkbox" checked={!!value} onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate(id, e.target.checked)} />;
      case FormFieldDataType.TIMESTAMP:
        //@ts-ignore
        return (<DateField label={label} name={label + '-Date'} mandatory={required} mode={isEditing && !readOnly ? 'edit' : 'view'} disabled={isEditing && readOnly} value={{ dateObject: new Date(value) }} disableUnderline={true} />);
      case FormFieldDataType.CONTROLLED_VOCABULARY:
        return (
          <select value={value} onChange={(e: React.ChangeEvent<HTMLSelectElement>) => onUpdate(id, e.target.value)}>
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
    </div>
  );
};