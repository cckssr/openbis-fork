import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { FormControl, MenuItem, Select, Grid2, InputLabel } from '@mui/material';

const Dropdown = ({
    label,
    values = [],
    initValue = '',
    isMulti = false,
    disabled = false,
    onSelectChange,
    mappingItemsCallback,
}) => {
    const [value, setValue] = useState(initValue);

    useEffect(() => {
        if (initValue !== value) setValue(initValue);
    }, [initValue]);

    const handleChange = useCallback((event) => {
        setValue(event.target.value);
        onSelectChange?.(event);
    }, [onSelectChange]);

    // Memoize menu items to prevent unnecessary re-renders
    const menuItems = useMemo(() => {
        return mappingItemsCallback
            ? mappingItemsCallback(values, label)
            : values.map((v, i) => (
                  <MenuItem key={`select-${label}-menuitem-${i}`} value={v}>
                      {v}
                  </MenuItem>
              ));
    }, [values, label, mappingItemsCallback]);

    return (
        <Grid2 container alignItems="center" sx={{ mb: 1, px: 1 }} size={12}>
            <FormControl fullWidth variant="standard" onClick={(e) => e.stopPropagation()}>
                <InputLabel id={`select-${label}-label`}>{label}</InputLabel>
                <Select
                    labelId={`select-${label}-label`}
                    id={`select-${label}`}
                    value={value}
                    multiple={isMulti}
                    label={label}
                    name={label}
                    onChange={handleChange}
                    disabled={disabled}
                >
                    {mappingItemsCallback 
                        ? mappingItemsCallback(values, label) 
                        : values.map((v, i) => (
                            <MenuItem 
                                key={`select-${label}-menuitem-${i}`} 
                                value={v} 
                            >
                                {v}
                            </MenuItem>
                        ))}
                </Select>
            </FormControl>
        </Grid2>
    );
};

export default Dropdown;
