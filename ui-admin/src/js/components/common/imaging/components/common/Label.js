import * as React from 'react';
import { Typography, Grid2 } from '@mui/material';

const Label = ({ label }) => {

    return (
        <Grid2 item='true' size={{ xs: 12, sm: 4 }} sx={{ textAlign: 'right'}}>
            <Typography id={'label-' + label + '-id'} noWrap={!label.includes(' ')} gutterBottom>
                {label}
            </Typography>
        </Grid2>
    );
}

export default Label;