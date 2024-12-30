import React from 'react';
import { FormControl, MenuItem, Select, Grid2, Typography } from '@mui/material';
import constants from '@src/js/components/common/imaging/constants.js';
import Label from '@src/js/components/common/imaging/components/common/Label.js';

const ColorItem = ({ colorMapValue }) => {
    return (
        <span style={{ background: `linear-gradient(90deg, ${constants.DEFAULT_COLORMAP[colorMapValue]})`, width: '70%', height: '15px', marginLeft: '10px' }} />
    )
}

const ColorMap = ({ values, initValue, label, disabled = false, onSelectChange = null }) => {
    const [value, setValue] = React.useState(initValue);

    React.useEffect(() => {
        //console.log('useEffect DROPDOWN: ', label, values, initValue, isMulti);
        if (initValue !== value)
            setValue(initValue);
    }, [initValue]);

    const handleChange = (event) => {
        setValue(event.target.value);
        if (onSelectChange != null) {
            onSelectChange(event);
        }
    };

    return (
        <Grid2 container spacing={2} direction='row' sx={{ alignItems: 'center', mb: 1, px: 1, width: '100%' }}>
            <Label label={label}/>
            <Grid2 item='true' size={{ xs: 12, sm: 8 }}>
                <FormControl fullWidth variant='standard' onClick={event => event.stopPropagation()}>
                    <Select
                        labelId={'select-' + label + '-label'}
                        disabled={disabled}
                        id={'select-' + label}
                        value={value}
                        defaultValue={initValue}
                        multiple={false}
                        label={label}
                        name={label}
                        onChange={handleChange}
                    >
                        {values.map((v, i) => <MenuItem key={'select-' + label + '-menuitem-' + i} value={v}>
                            <span style={{ width: '30%' }}>{v}</span> <ColorItem colorMapValue={v} />
                        </MenuItem>)}
                    </Select>
                </FormControl>
            </Grid2>
        </Grid2>
    )
}

export default ColorMap;