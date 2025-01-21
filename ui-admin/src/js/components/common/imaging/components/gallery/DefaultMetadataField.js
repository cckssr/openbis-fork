import React from "react";
import { isObjectEmpty } from "@src/js/components/common/imaging/utils.js"
import makeStyles from "@mui/styles/makeStyles";
import TextField from '@src/js/components/common/form/TextField.jsx'

const useStyles = makeStyles((theme) => ({
    field: {
        paddingBottom: theme.spacing(1)
    }
}));

const DefaultMetadataField = ({label, value, mode = 'view', disabled = true}) => {
    const classes = useStyles();
    if (!isObjectEmpty(value) || value.length > 0)
        return <div className={classes.field}>
                    <TextField label={label}
                        value={value}
                        disabled={disabled}
                        mode={mode}
                    />
                </div>
}

export default DefaultMetadataField;