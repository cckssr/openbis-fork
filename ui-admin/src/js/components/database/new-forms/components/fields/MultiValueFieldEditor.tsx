import React, { useState, useEffect, useRef } from 'react';
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

type Slot = { id: number; value: any };

const defaultIsEmpty = (v: any) => v === null || v === undefined || v === '';

const MultiValueFieldEditor: React.FC<MultiValueFieldEditorProps> = ({
  label,
  required,
  values,
  onChange,
  renderInput,
  isEmpty = defaultIsEmpty
}) => {
  const nextIdRef = useRef(0);

  const nextId = (): number => {
    nextIdRef.current += 1;
    return nextIdRef.current;
  };

  const initSlots = (): Slot[] => {
    const base: Slot[] = values && values.length > 0
      ? values.map(v => ({ id: nextId(), value: v }))
      : [];
    if (base.length === 0 || !isEmpty(base[base.length - 1].value)) {
      base.push({ id: nextId(), value: null });
    }
    return base;
  };

  const [slots, setSlots] = useState<Slot[]>(initSlots);

  // Re-sync slots when the `values` prop changes externally (e.g. auto-save restore from
  // localStorage). Compare by content so the user's own edits — which round-trip through the
  // parent and arrive back as a new `values` reference on every keystroke — don't reset slots.
  useEffect(() => {
    const nonEmpty = slots.filter(s => !isEmpty(s.value)).map(s => s.value);
    if (JSON.stringify(nonEmpty) !== JSON.stringify(values)) {
      setSlots(initSlots());
    }
  }, [values]);

  const handleChange = (index: number, newVal: any) => {
    const next = slots.map((s, i) => i === index ? { ...s, value: newVal } : s);
    if (index === next.length - 1 && !isEmpty(newVal)) {
      next.push({ id: nextId(), value: null });
    }
    setSlots(next);
    onChange(next.filter(s => !isEmpty(s.value)).map(s => s.value));
  };

  const handleRemove = (index: number) => {
    if (slots.length === 1) {
      setSlots([{ id: nextId(), value: null }]);
      onChange([]);
    } else {
      const next = slots.filter((_, i) => i !== index);
      if (!isEmpty(next[next.length - 1].value)) {
        next.push({ id: nextId(), value: null });
      }
      setSlots(next);
      onChange(next.filter(s => !isEmpty(s.value)).map(s => s.value));
    }
  };

  return (
    <Box>
      <Typography variant="body2">
        <FormFieldLabel label={label} mandatory={required} />
      </Typography>
      {slots.map((slot, i) => (
        <Box
          key={slot.id}
          sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5, marginBottom: 0 }}
        >
          <Box sx={{
            flex: 1, minWidth: 0, fontSize: '0.875rem',
            ...(i > 0 && {
              '& .MuiFilledInput-root': { borderTopLeftRadius: 0, borderTopRightRadius: 0 },
              '& .SourceCodeField-editContainer': { borderTopLeftRadius: 0, borderTopRightRadius: 0 },
              '& .SourceCodeField-editContainer textarea': {
                borderTopLeftRadius: '0 !important',
                borderTopRightRadius: '0 !important',
              },
            })
          }}>
            {renderInput(slot.value, (newVal: any) => handleChange(i, newVal))}
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
