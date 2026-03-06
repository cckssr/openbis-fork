import React from 'react'
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts'
import TextField from '@src/js/components/common/form/TextField.jsx'
import {FormFieldDataType, FormMode} from '@src/js/components/database/new-forms/types/formEnums.ts'

export const TextFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  onFieldChange,
  mode
}) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE
  const isArrayField = (field.dataType === FormFieldDataType.ARRAY_TIMESTAMP ||
          field.dataType === FormFieldDataType.ARRAY_STRING ||
          field.dataType === FormFieldDataType.ARRAY_INTEGER ||
          field.dataType === FormFieldDataType.ARRAY_REAL)

  return (
    <TextField
      mandatory={field.required}
      label={field.label}
      mode={isEditing && !field.readOnly ? FormMode.EDIT : FormMode.VIEW}
      disabled={isEditing && field.readOnly}
      dataType={field.dataType}
      value={field.value}
      startAdornment={isArrayField ? "[" : null}
      endAdornment={isArrayField ? "]" : null}
      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
        onFieldChange(field.id, e.target.value)
      }
      disableUnderline={true}
    />
  )
}