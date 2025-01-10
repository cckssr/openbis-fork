import * as React from 'react';
import { createTheme, styled, ThemeProvider, StyledEngineProvider } from '@mui/material/styles';
import makeStyles from '@mui/styles/makeStyles';
import Box from '@mui/material/Box';
import Slider from '@mui/material/Slider';
import IconButton from '@mui/material/IconButton';
import PauseRounded from '@mui/icons-material/PauseRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import FastForwardRounded from '@mui/icons-material/FastForwardRounded';
import FastRewindRounded from '@mui/icons-material/FastRewindRounded';
import MobileStepper from "@mui/material/MobileStepper";

const themeDisabled = createTheme({
    overrides: {
        // Style sheet name ⚛️
        MuiIconButton: {
            // Name of the rule
            root: {
                // Some CSS
                color: 'rgba(0, 0, 0, 1)'
            }
        },
    },
});

const themeSlider = createTheme({
   overrides: {
       MuiSlider: {
           thumb: {
               width: '8px',
               height: '8px',
               marginTop: '-3px',
           },
           markLabel: {
               top: '20px',
               fontSize: '0.6rem',
           },
           marked:{
               marginBottom: "unset"
           }
       }
   }
});

const Widget = styled('div')(() => ({
    padding: 10,
    borderRadius: 16,
    maxWidth: '100%',
    height: 'fit-content',
    position: 'relative',
    zIndex: 1,
    backgroundColor: 'rgba(111,111,111,0.2)',
    backdropFilter: 'blur(40px)',
}));

const defaultSpeeds = [
    {
        value: 2000,
        label: '2s',
    },
    {
        value: 5000,
        label: '5s',
    },
    {
        value: 10000,
        label: '10s',
    },
];

const useStyles = makeStyles({
    rootBox: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    root: {
        justifyContent: 'center',
        padding: 'unset',
        background: 'unset'
    }
});
export default function Player({ label= 'DEFAULT', onStep, steps = [], speeds = defaultSpeeds, speedable = false }) {
    const classes = useStyles();
    const [activeStep, setActiveStep] = React.useState(-1);
    const [paused, setPaused] = React.useState(true);
    const [speed, setSpeed] = React.useState(2000);
    const timeoutRef = React.useRef(null);

    const handleSpeedChange = (event, newValue) => {
        setSpeed(newValue);
    };

    function resetTimeout() {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }
    }

    React.useEffect(() => {
        if (!paused){
            //console.log(`RUN activeStep=${activeStep} value=${steps[activeStep]}`);
            resetTimeout();
            onStep(steps[activeStep], label, true);
            timeoutRef.current = setTimeout(
                () =>
                    setActiveStep((prevIndex) =>
                        prevIndex === steps.length - 1 ? 0 : prevIndex + 1
                    ),
                speed
            );

            return () => {
                resetTimeout();
            };
        }
    }, [activeStep]);

    const handleNext = () => {
        setActiveStep((prevActiveStep) => prevActiveStep + 1);
    };

    const handleBack = () => {
        setActiveStep((prevActiveStep) => prevActiveStep - 1);
    };

    const handlePlay = () => {
        setPaused(!paused);
        if (paused) {
            resetTimeout();
            timeoutRef.current = setTimeout(
                () =>
                    setActiveStep((prevIndex) =>
                        prevIndex === steps.length - 1 ? 0 : prevIndex + 1
                    ),
                2000
            );
        }
        return () => {
            resetTimeout();
        };
    }

    return (
        (<Widget>
            <Box className={classes.rootBox}>
                <StyledEngineProvider injectFirst>
                    <ThemeProvider theme={themeDisabled}>
                        <IconButton aria-label="previous"
                                    onClick={handleBack}
                                    disabled={paused || activeStep <= 0}
                                    size="small"
                        >
                            <FastRewindRounded />
                        </IconButton>
                        <IconButton aria-label={paused ? 'play' : 'pause'}
                                    onClick={handlePlay}
                                    size="small"
                        >
                            {paused ? <PlayArrowRounded /> : <PauseRounded />}
                        </IconButton>
                        <IconButton aria-label="next"
                                    onClick={handleNext}
                                    disabled={paused || activeStep === steps.length-1}
                                    size="small"
                        >
                            <FastForwardRounded />
                        </IconButton>
                    </ThemeProvider>
                </StyledEngineProvider>
            </Box>
            {!paused && <MobileStepper variant="text"
                                       steps={steps.length}
                                       position="static"
                                       activeStep={activeStep}
                                       className={classes.root}
            />}
            <StyledEngineProvider injectFirst>
                <ThemeProvider theme={themeSlider}>
                    {speedable && <Slider
                        className={classes.thumb}
                        value={speed}
                        color="primary"
                        onChange={handleSpeedChange}
                        step={null}
                        min={defaultSpeeds[0].value}
                        max={defaultSpeeds[defaultSpeeds.length - 1].value}
                        marks={defaultSpeeds}
                    />}
                </ThemeProvider>
            </StyledEngineProvider>
        </Widget>)
    );
}