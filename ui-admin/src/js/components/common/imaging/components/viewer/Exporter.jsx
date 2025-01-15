import { Box, Modal, Typography } from '@mui/material';
import React from 'react';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import makeStyles from '@mui/styles/makeStyles';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import messages from '@src/js/common/messages.js';
import constants from '@src/js/components/common/imaging/constants.js';
import Button from '@src/js/components/common/form/Button.jsx';
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';

const style = {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 400,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
};

const useStyles = makeStyles((theme) => ({
    risky: {
        backgroundColor: theme.palette.error.main,
        color: theme.palette.error.contrastText,
        '&:hover': {
            backgroundColor: theme.palette.error.dark
        },
        '&:disabled': {
            backgroundColor: theme.palette.error.light
        }
    },
    mt: {
        marginTop: '10px'
    }
}));


const Exporter = ({ config, handleExport, disabled = false, styles }) => {
    const classes = useStyles();
    const [open, setOpen] = React.useState(false);
    const [exportState, setExportState] = React.useState(Object.fromEntries(config.map(c => {
        switch (c.type) {
            case constants.DROPDOWN:
                return [c.label, c.multiselect ? [c.values[0]] : c.values[0]];
        }
    })));

    const handleOpen = (event) => {
        event.stopPropagation();
        setOpen(true);
    };
    const handleClose = (event) => {
        event.stopPropagation();
        setOpen(false);
    }

    const handleExportChange = (event) => {
        event.stopPropagation();
        setExportState(prevState => {
            let newState = { ...prevState };
            newState[event.target.name] = event.target.value;
            return newState;
        });
    };

    const sendExportRequest = (event) => {
        handleExport(exportState);
        handleClose(event);
    };

    if (isObjectEmpty(exportState)) return null;

    return (<>
        <Button label={messages.get(messages.EXPORT)}
            type='final'
            color='inherit'
            variant='outlined'
            disabled={disabled}
            onClick={handleOpen}
            startIcon={<CloudDownloadIcon />}
            styles={styles} />
        <Modal open={open}
            onClose={handleClose}
            aria-labelledby='modal-modal-title'
            aria-describedby='modal-modal-description'
        >
            <Box sx={style}>
                <Typography id='modal-modal-title' variant='h6' component='h2'>
                    {messages.get(messages.EXPORT_SELECTION)}
                </Typography>
                {config.map((c, idx) => {
                    switch (c.type) {
                        case constants.DROPDOWN:
                            return <Dropdown key={'export-' + c.type + '-' + idx}
                                label={c.label}
                                initValue={exportState[c.label]}
                                values={c.values}
                                isMulti={c.multiselect}
                                onSelectChange={handleExportChange} />
                        default:
                            return (<Typography variant='body2'>
                                {messages.get(messages.NO_PREVIEW)}: {c.type}
                            </Typography>)
                    }
                })}
                <div className={classes.mt} >
                    <Button label={messages.get(messages.EXPORT)} type='secondary' onClick={event => sendExportRequest(event)} />
                    <Button label={messages.get(messages.CANCEL)} type='risky' onClick={handleClose} styles={{ root: classes.risky }} />
                </div>
            </Box>

        </Modal>
    </>);
};

export default Exporter;