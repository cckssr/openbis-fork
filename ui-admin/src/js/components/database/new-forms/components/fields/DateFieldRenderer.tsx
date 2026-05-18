import React from 'react'
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts'
import DateField from '@src/js/components/common/form/DateField.jsx'
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx'
import MultiValueFieldEditor from './MultiValueFieldEditor.tsx'
import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts'
import date from '@src/js/common/date.js'

export const DateFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
  const isEditing = mode === 'edit' || mode === 'create'
  const dateTime = field.dataType === FormFieldDataType.TIMESTAMP

  if (field.isMultiValue && !isEditing) {
    const values: any[] = Array.isArray(field.value) ? field.value : [];
    const lines = values.map((v, i) => {
      const d = v ? new Date(v) : null;
      const text = d && !isNaN(d.getTime())
        ? date.format(d, dateTime)
        : String(v ?? '');
      return <div key={i}>{text}</div>;
    });
    return (
      <FormFieldView
        label={field.label}
        value={lines.length > 0 ? <>{lines}</> : undefined}
        disableUnderline={true}
      />
    );
  } else if (field.isMultiValue && isEditing && !field.readOnly) {
    return (
      <MultiValueFieldEditor
        label={field.label}
        required={field.required}
        values={Array.isArray(field.value) ? field.value : []}
        onChange={(vals) => onFieldChange(field.id, vals)}
        renderInput={(val, onChange) => (
          // @ts-ignore
          <DateField
            label={null}
            mode="edit"
            value={{ dateObject: val ? new Date(val) : null }}
            onChange={(e: any) => onChange(e.target.value.dateString || null)}
            dateTime={dateTime}
            disableUnderline={true}
          />
        )}
        isEmpty={(v) => !v}
      />
    );
  } else {
    return (
        //@ts-ignore
        <DateField
            label={field.label}
            mandatory={field.required}
            mode={isEditing && !field.readOnly ? 'edit' : 'view'}
            disabled={isEditing && field.readOnly}
            value={{dateObject: field.value ? new Date(field.value) : null}}
            onChange={(e: React.ChangeEvent<any>) =>
                onFieldChange(field.id, e.target.value.dateString)
            }
            dateTime={dateTime}
            disableUnderline={true}
        />
    )
  }
}