import React from 'react'
// @ts-ignore
import withStyles from '@mui/styles/withStyles'
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts'
import TextField from '@src/js/components/common/form/TextField.jsx'
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx'
import MultiValueFieldEditor from './MultiValueFieldEditor.tsx'
import MuiTextField from '@mui/material/TextField'
import {FormFieldDataType, FormMode} from '@src/js/components/database/new-forms/types/formEnums.ts'
import date from '@src/js/common/date.js'

const styles = (theme: any) => ({
  input: {
    fontSize: theme.typography.body2.fontSize
  }
})

const TextFieldRendererBase: React.FC<FieldRendererProps & { classes: any }> = ({
  field,
  onFieldChange,
  mode,
  classes
}) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE

  if (field.isMultiValue && !isEditing) {
    const values: any[] = Array.isArray(field.value) ? field.value : [];
    const lines = values.map((v, i) => {
      let text: string;
      if (Array.isArray(v)) {
        if (field.dataType === FormFieldDataType.ARRAY_TIMESTAMP) {
          text = `["${v.map(item => date.format(new Date(item), true)).join('", "')}"]`;
        } else if (field.dataType === FormFieldDataType.ARRAY_INTEGER ||
          field.dataType === FormFieldDataType.ARRAY_REAL) {
          text = '[' + v.map(Number).join(', ') + ']';
        } else {
          text = '[' + v.map(item => JSON.stringify(item)).join(', ') + ']';
        }
      } else {
        text = String(v ?? '');
      }
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
    const isArrayType = field.dataType === FormFieldDataType.ARRAY_INTEGER
      || field.dataType === FormFieldDataType.ARRAY_REAL
      || field.dataType === FormFieldDataType.ARRAY_STRING
      || field.dataType === FormFieldDataType.ARRAY_TIMESTAMP;
    const isNumeric = !isArrayType && (
      field.dataType === FormFieldDataType.INTEGER
      || field.dataType === FormFieldDataType.REAL
    );
    const formatInner = (val: any): string => {
      if (val == null) return '';
      if (Array.isArray(val)) {
        if (field.dataType === FormFieldDataType.ARRAY_TIMESTAMP) {
          return `["${val.map(item => date.format(new Date(item), true)).join('", "')}"]`;
        } else if (field.dataType === FormFieldDataType.ARRAY_INTEGER ||
          field.dataType === FormFieldDataType.ARRAY_REAL) {
          return '[' + val.map(Number).join(', ') + ']';
        } else {
          return '[' + val.map(item => JSON.stringify(item)).join(', ') + ']';
        }
      } else {
        return String(val);
      }
    };
    return (
      <MultiValueFieldEditor
        label={field.label}
        required={field.required}
        values={Array.isArray(field.value) ? field.value : []}
        onChange={(vals) => onFieldChange(field.id, vals)}
        renderInput={(val, onChange) => (
          <MuiTextField
            variant="filled"
            size="small"
            fullWidth
            hiddenLabel
            type={isNumeric ? 'number' : 'text'}
            value={formatInner(val)}
            onChange={(e) => onChange(e.target.value)}
            margin="dense"
            slotProps={{
              htmlInput: { className: classes.input }
            }}
          />
        )}
      />
    );
  } else {
    const value: string | string[] | number[] | null = field.value && field.value.map &&
        (field.dataType === FormFieldDataType.ARRAY_INTEGER ||
            field.dataType === FormFieldDataType.ARRAY_REAL)
        ? field.value.map(Number) : field.value
    return (
        <TextField
            mandatory={field.required}
            label={field.label}
            mode={isEditing && !field.readOnly ? FormMode.EDIT : FormMode.VIEW}
            disabled={isEditing && field.readOnly}
            dataType={field.dataType}
            value={value}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                onFieldChange(field.id, e.target.value)
            }
            disableUnderline={true}
        />
    );
  }
}

export const TextFieldRenderer = withStyles(styles)(TextFieldRendererBase)