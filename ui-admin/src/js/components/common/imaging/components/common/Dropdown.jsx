import * as React from 'react';
import { FormControl, MenuItem, Select, Grid2, InputLabel } from '@mui/material';

const Dropdown = ({ label, values, initValue, isMulti, disabled = false, onSelectChange = null, mappingItemsCallback = null }) => {
    const [value, setValue] = React.useState(initValue);

    React.useEffect(() => {
        //console.log('useEffect DROPDOWN: ', label, values, initValue, typeof initValue === 'string', isMulti);
        if (initValue !== value) setValue(initValue);
    }, [initValue]);

    const handleChange = (event) => {
        setValue(event.target.value);
        if (onSelectChange != null) {
            onSelectChange(event);
        }
    };

    return (
        <Grid2 container spacing={2} direction='row' sx={{ alignItems: 'center', mb: 1, px: 1 }} size={{ xs: 12, sm: 12 }}>
            <FormControl fullWidth variant='standard' onClick={event => event.stopPropagation()}>
                <InputLabel id={'select-' + label + '-label'}>{label}</InputLabel>
                <Select labelId={'select-' + label + '-label'}
                    id={'select-' + label}
                    defaultValue={initValue}
                    value={value}
                    multiple={isMulti}
                    label={label}
                    name={label}
                    onChange={handleChange}
                    disabled={disabled}
                >
                    {mappingItemsCallback ? mappingItemsCallback(values, label) : values.map((v, i) => <MenuItem key={'select-' + label + '-menuitem-' + i} value={v} onClick={event => event.stopPropagation()}>{v}</MenuItem>)}
                </Select>
            </FormControl>
        </Grid2>
    );
}

export default Dropdown;