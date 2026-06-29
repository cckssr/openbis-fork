import React from 'react';
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import SpreadsheetField from '@src/js/components/common/form/SpreadsheetField.jsx';

/**
 * Renders a SPREADSHEET property using jspreadsheet community edition.
 *
 * `field.value` is a Spreadsheet DTO instance (or '' when empty) for single-value properties, and
 * an array of such instances for multi-value properties. Changes are emitted back through
 * `onFieldChange` as the plain Spreadsheet model, which the DTO setter serializes to
 * `<DATA>base64</DATA>`.
 */
export const SpreadsheetFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  onFieldChange,
  mode
}) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
  const editable = isEditing && !field.readOnly;

  const label = (
    <Typography
      variant="body2"
      component="div"
      sx={{ color: '#0000008a', fontSize: '0.7rem' }}
    >
      {field.label} {field.required ? '*' : ''}
    </Typography>
  );

  // Multi-value spreadsheets are rare; render each entry read-only (no multi-value editing UI).
  if (field.isMultiValue) {
    const values: any[] = Array.isArray(field.value) ? field.value : [];
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {label}
        {values.map((value, index) => (
          <SpreadsheetField key={index} value={value} editable={false} />
        ))}
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
      {label}
      <SpreadsheetField
        key={editable ? 'edit' : 'view'}
        value={field.value}
        editable={editable}
        onChange={(spreadsheet: any) => onFieldChange(field.id, spreadsheet)}
      />
    </Box>
  );
};
