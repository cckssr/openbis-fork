import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { FormControl, MenuItem, Select, Grid2, InputLabel } from '@mui/material';
import InfoOntology from '@src/js/components/common/imaging/components/viewer/InfoOntology.js';

const Dropdown = ({
    label,
    values = [],
    initValue = '',
    isMulti = false,
    disabled = false,
    onSelectChange,
    mappingItemsCallback,
    semanticAnnotation
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
        <Grid2 container alignItems="center" direction='row' sx={{ mb: 1, px: 1 }} size={12}>
            <Grid2 size={'grow'}>
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
                        {menuItems}
                    </Select>
                </FormControl>
            </Grid2>
            <Grid2 size={'auto'}>
                <InfoOntology semanticAnnotation={semanticAnnotation} />
            </Grid2>
        </Grid2>
    );
};

export default Dropdown;
