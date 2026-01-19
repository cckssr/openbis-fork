import React from 'react'
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts'
import DateField from '@src/js/components/common/form/DateField.jsx'
import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts'

export const DateFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
  const isEditing = mode === 'edit' || mode === 'create'

  return (
    //@ts-ignore
    <DateField
      label={field.label}
      mandatory={field.required}
      mode={isEditing && !field.readOnly ? 'edit' : 'view'}
      disabled={isEditing && field.readOnly}
      value={{ dateObject: field.value ? new Date(field.value) : null }}
      onChange={(e: React.ChangeEvent<any>) =>
        onFieldChange(field.id, e.target.value.dateString)
      }
      dateTime={field.dataType === FormFieldDataType.TIMESTAMP}
      disableUnderline={true}
    />
  )
}