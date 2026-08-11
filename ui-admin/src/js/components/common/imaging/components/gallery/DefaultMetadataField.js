import React from "react";
import { isObjectEmpty } from "@src/js/components/common/imaging/utils.js"
import makeStyles from "@mui/styles/makeStyles";
import TextField from '@src/js/components/common/form/TextField.jsx'

const useStyles = makeStyles((theme) => ({
    field: {
        paddingBottom: theme.spacing(1)
    }
}));

const isFileLikeObject = (value) => {
    return value && typeof value === 'object' &&
        typeof value.name === 'string' &&
        'size' in value &&
        'type' in value &&
        'lastModified' in value;
}

export const formatMetadataValue = (value) => {
    if (React.isValidElement(value)) {
        return value;
    }

    if (Array.isArray(value)) {
        if (value.some(item => React.isValidElement(item))) {
            return value.map(item => formatMetadataValue(item));
        } else {
            return value.map(item => formatMetadataValue(item)).join(', ');
        }
    }

    if (value instanceof Date) {
        return value.toISOString();
    }

    if (isFileLikeObject(value)) {
        return value.webkitRelativePath || value.name;
    }

    if (value && typeof value === 'object') {
        try {
            return JSON.stringify(value);
        } catch (error) {
            return String(value);
        }
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }

    return value;
}

export const hasMetadataValue = (value) => {
    return value !== null &&
        value !== undefined &&
        !isObjectEmpty(value) &&
        (!Array.isArray(value) || value.length > 0) &&
        formatMetadataValue(value) !== '';
}

const DefaultMetadataField = ({label, value, mode = 'view', disabled = true}) => {
    const classes = useStyles();
    if (hasMetadataValue(value))
        return <div className={classes.field}>
                    <TextField label={label}
                        value={formatMetadataValue(value)}
                        disabled={disabled}
                        mode={mode}
                    />
                </div>
}

export default DefaultMetadataField;
