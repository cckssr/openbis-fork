import React from 'react';
import Switch from '@mui/material/Switch';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';

export default function CustomSwitch({ label = 'default', labelPlacement = null, size = 'medium', isChecked = true, onChange, disabled = false}) {
    const toggleChecked = (event) => {
        onChange(event.target.checked);
    };

    if (labelPlacement)
        return <FormGroup>
            <FormControlLabel
                name='default-control-switch'
                control={<Switch size={size} checked={isChecked} onChange={event => toggleChecked(event)} color='primary'/>}
                disabled={disabled}
                label={label}
                labelPlacement={labelPlacement}
            />
        </FormGroup>
    else
        return <Switch disabled={disabled} name='default-switch' size={size} checked={isChecked} onChange={event => toggleChecked(event)} color='primary'/>;
}