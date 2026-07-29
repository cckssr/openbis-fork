import React from 'react';
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import SpreadsheetField from '@src/js/components/common/form/SpreadsheetField.jsx';
import MultiValueFieldEditor from '@src/js/components/database/new-forms/components/fields/MultiValueFieldEditor.tsx';

/**
 * Renders a SPREADSHEET property using jspreadsheet community edition.
 *
 * `field.value` is a plain Spreadsheet model object (or '' when empty) for single-value
 * properties, and an array of such objects for multi-value properties. Changes are emitted
 * back through `onFieldChange` as the plain Spreadsheet model, which the DTO setter serializes
 * to `<DATA>base64</DATA>`.
 */
export const SpreadsheetFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  onFieldChange,
  mode
}) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
  const editable = isEditing && !field.readOnly;

  const label: React.JSX.Element = (
    <Typography
      variant="body2"
      component="div"
      sx={{ color: '#0000008a', fontSize: '0.7rem' }}
    >
      {field.label} {field.required ? '*' : ''}
    </Typography>
  );

  if (field.isMultiValue) {
    if (editable) {
      return (
        <MultiValueFieldEditor
          required={field.required}
          values={Array.isArray(field.value) ? field.value : []}
          onChange={(vals) => onFieldChange(field.id, vals)}
          renderInput={(val, index, handleChange) => (
            [<>{index === 0 ? label : null}</>,
            <SpreadsheetField
              value={val}
              editable={true}
              onChange={handleChange}
            />]
          )}
          isEmpty={(v) => v === null || v === undefined}
        />
      );
    }

    const values: any[] = Array.isArray(field.value) ? field.value : [];
    return (
      <Box sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 1,
        fontFamily: '"Helvetica Neue",Helvetica,Arial,sans-serif;'
      }}>
        {label}
        {values.map((value, index) => (
          <SpreadsheetField key={index} value={value} editable={false} />
        ))}
      </Box>
    );
  } else {
    return (
        <Box sx={{
          display: 'flex',
          flexDirection: 'column',
          gap: 0.5,
          fontFamily: '"Helvetica Neue",Helvetica,Arial,sans-serif;'
        }}>
          {label}
          <SpreadsheetField
              key={editable ? 'edit' : 'view'}
              value={field.value}
              editable={editable}
              onChange={(spreadsheet: any) => onFieldChange(field.id, spreadsheet)}
          />
        </Box>
    );
  }
};
