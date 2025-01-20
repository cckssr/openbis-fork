import * as React from 'react';
import ConfirmationDialog from "@src/js/components/common/dialog/ConfirmationDialog.jsx";
import Button from "@src/js/components/common/form/Button.jsx";
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles((theme) => ({
    button: {
        marginLeft: theme.spacing(1)
    }
}));

export default function AlertDialog({label, icon, title, content, disabled, onHandleYes}) {
    const [open, setOpen] = React.useState(false);

    const classes = useStyles();

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleYes = () => {
        setOpen(false);
        onHandleYes(true);
    };

    return (<>
        <Button
            label={label}
            variant='outlined'
            color='inherit'
            onClick={handleClickOpen}
            startIcon={icon}
            disabled={disabled} />
        <ConfirmationDialog open={open}
                            onConfirm={handleYes}
                            onCancel={() => setOpen(false)}
                            title={title}
                            content={content}/>
    </>);
}