import * as React from 'react';
import { InputAdornment, Input, Grid2, Slider, FormHelperText, InputLabel } from "@mui/material";
import Player from "@src/js/components/common/imaging/components/common/Player.jsx";
import { makeStyles } from '@mui/styles';
import InfoOntology from '@src/js/components/common/imaging/components/viewer/InfoOntology.js';

const useStyles = makeStyles(theme => ({
    showText: {
        '&:hover': {
            overflow: 'unset'
        }
    }
}));

const InputSlider = ({ label, range, initValue, playable, speeds, disabled = false, onChange, unit = null, semanticAnnotation}) => {
    const classes = useStyles();

    const min = Number(range[0])
    const max = Number(range[1])
    const step = Number(range[2])
    const arrayRange = Array.from(
        { length: (max - min) / step + 1 },
        (value, index) => min + index * step
    );
    const [value, setValue] = React.useState(initValue == null ? min : Number(initValue));

    function roundToClosest(counts, goal) {
        return counts.reduce((prev, curr) => Math.abs(curr - goal) < Math.abs(prev - goal) ? curr : prev);
    }

    const handleSliderChange = (newValue, name, update) => {
        setValue(newValue);
        onChange(name, [newValue], update);
    };

    const handleInputChange = (event) => {
        let newValue = event.target.value === '' ? 0 : Number(event.target.value);
        setValue(newValue);
        onChange(event.target.name, [newValue]);
    };

    const handleBlur = (event) => {
        let newValue = value;
        if (value < min) {
            newValue = min;
        } else if (value > max) {
            newValue = max;
        } else if (!arrayRange.includes(value)) {
            newValue = roundToClosest(arrayRange, value);
        }
        setValue(newValue);
        onChange(event.target.name, [newValue]);
    };

    return (
        <Grid2 container spacing={2} direction='row' sx={{ alignItems: 'center', mb: 1, px: 1 }} size={{ xs: 12, sm: 12 }}>
            <Grid2 size={{ sm: 4 }}>
                <InputLabel className={classes.showText} htmlFor='single-input-helper-text'>{label}</InputLabel>
                <Input id='single-input-helper-text'
                    value={initValue == null ? min : Number(initValue)}
                    size="small"
                    name={label}
                    onChange={handleInputChange}
                    onBlur={handleBlur}
                    disabled={disabled}
                    aria-describedby='single-input-helper-text'
                    endAdornment={unit && <InputAdornment position="end">{unit}</InputAdornment>}
                    inputProps={{
                        step: step,
                        min: min,
                        max: max,
                        type: 'number'
                    }}
                />
                <FormHelperText id='single-input-helper-text'>{unit}</FormHelperText>
            </Grid2>
            <Grid2 size={{ sm: 8 }} sx={{ pr: 1 }}>
                <Slider name={label}
                    value={initValue == null ? min : Number(initValue)}
                    onChange={(event, newValue) => handleSliderChange(newValue, label, false)}
                    min={min}
                    max={max}
                    step={step}
                    disabled={disabled}
                />
            </Grid2>
            <Grid2 size={'auto'}>
                <InfoOntology semanticAnnotation={semanticAnnotation} />
            </Grid2>
            {playable &&
                (<Grid2 size='grow'>
                    <Player label={label} onStep={handleSliderChange} steps={arrayRange} speeds={speeds} speedable={playable} />
                </Grid2>)
            }
        </Grid2>
    );
}

export default InputSlider;