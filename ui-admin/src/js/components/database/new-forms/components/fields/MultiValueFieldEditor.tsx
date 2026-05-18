import React, { useState, useEffect } from 'react';
import { Box, IconButton } from '@mui/material';
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutline';
import FormFieldLabel from '@src/js/components/common/form/FormFieldLabel.jsx';
import Typography from "@mui/material/Typography";

interface MultiValueFieldEditorProps {
  label: string;
  required?: boolean;
  values: any[];
  onChange: (values: any[]) => void;
  renderInput: (value: any, onChange: (newVal: any) => void) => React.ReactNode;
  isEmpty?: (value: any) => boolean;
}

const defaultIsEmpty = (v: any) => v === null || v === undefined || v === '';

const MultiValueFieldEditor: React.FC<MultiValueFieldEditorProps> = ({
  label,
  required,
  values,
  onChange,
  renderInput,
  isEmpty = defaultIsEmpty
}) => {
  const initSlots = (): any[] => {
    const base = values && values.length > 0 ? [...values] : [];
    if (base.length === 0 || !isEmpty(base[base.length - 1])) {
      base.push(null);
    }
    return base;
  };

  const [slots, setSlots] = useState<any[]>(initSlots);

  // Re-sync slots when the `values` prop changes externally (e.g. auto-save restore from
  // localStorage). Compare by content so the user's own edits — which round-trip through the
  // parent and arrive back as a new `values` reference on every keystroke — don't reset slots.
  useEffect(() => {
    const nonEmpty = slots.filter(v => !isEmpty(v));
    if (JSON.stringify(nonEmpty) !== JSON.stringify(values)) {
      setSlots(initSlots());
    }
  }, [values]);

  const handleChange = (index: number, newVal: any) => {
    const next = [...slots];
    next[index] = newVal;
    // When the user fills the trailing empty slot, append a new empty slot
    if (index === next.length - 1 && !isEmpty(newVal)) {
      next.push(null);
    }
    setSlots(next);
    onChange(next.filter(v => !isEmpty(v)));
  };

  const handleRemove = (index: number) => {
    if (slots.length === 1) {
      // Clear the only row rather than removing it
      setSlots([null]);
      onChange([]);
    } else {
      const next = slots.filter((_, i) => i !== index);
      // Always keep a trailing empty slot
      if (!isEmpty(next[next.length - 1])) {
        next.push(null);
      }
      setSlots(next);
      onChange(next.filter(v => !isEmpty(v)));
    }
  };

  return (
    <Box>
      <Typography variant="body2">
        <FormFieldLabel label={label} mandatory={required} />
      </Typography>
      {slots.map((val, i) => (
        <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
          <Box sx={{ flex: 1 }}>
            {renderInput(val, (newVal: any) => handleChange(i, newVal))}
          </Box>
          <IconButton size="small" onClick={() => handleRemove(i)}>
            <RemoveCircleOutlineIcon fontSize="small" />
          </IconButton>
        </Box>
      ))}
    </Box>
  );
};

export default MultiValueFieldEditor;
