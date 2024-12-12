import React from 'react';
import { FormControl, MenuItem, Select, Grid2 } from "@mui/material";
import GridFilterOptions from '@src/js/components/common/grid/GridFilterOptions.js';
import TextField from '@src/js/components/common/form/TextField.jsx';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import messages from '@src/js/common/messages.js';
import { makeStyles } from '@mui/styles';
import constants from '@src/js/components/common/imaging/constants.js';

const useStyles = makeStyles((theme) => ({
    container: {
        display: 'flex',
        alignItems: 'center',
    },
    operator: {
        flex: '0 0 auto',
        marginRight: theme.spacing(2),
    },
    text: {
        width: '100%',
    },
    tagsSelect: {
        width: '100%',
        marginTop: theme.spacing(2)
    }
}));

const GalleryFilter = ({ id, options, galleryFilter, onGalleryFilterChange }) => {
    const classes = useStyles();
    const [selectedTags, setSelectedTags] = React.useState([]);
    const [selectedValue, setSelectedValue] = React.useState([]);

    const handleSelectValue = (event) => {
        setSelectedValue(event.target.value);
        handleGalleryFilterChange(event);
    };

    const handleGalleryFilterChange = (event) => {
        const { name, value } = event.target;
        if (onGalleryFilterChange) {
            const newGlobalFilter = { ...galleryFilter }
            newGlobalFilter[name] = value
            onGalleryFilterChange(newGlobalFilter)
        }
    };

    const handleTagsOnChange = (event) => {
        const { target: { value, name } } = event;
        setSelectedTags(typeof value === 'string' ? value.split(',') : value,);
        if (onGalleryFilterChange) {
            const newGlobalFilter = { ...galleryFilter }
            newGlobalFilter['text'] = value.join(' ')
            onGalleryFilterChange(newGlobalFilter)
        }
    };

    const renderInputFilter = () => {
        switch (galleryFilter.property) {
            case constants.IMAGING_TAGS:
                return <FormControl variant="standard" className={classes.tagsSelect} >
                    <Select multiple
                        value={selectedTags}
                        onChange={handleTagsOnChange}
                    >
                        {options.find(option => option.value === constants.IMAGING_TAGS).options.map((tag) => (
                            <MenuItem key={tag.value} value={tag.value}>
                                {tag.label}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
            case constants.DEFAULT_DATASET_VIEW:
                return <FormControl variant="standard" className={classes.tagsSelect} >
                    <Select value={selectedValue}
                        onChange={handleSelectValue}
                    >
                        {options.find(option => option.value === constants.DEFAULT_DATASET_VIEW).options.map((opt) => (
                            <MenuItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
            default:
                return <TextField name="text"
                    id={`${id}.gallery-filter`}
                    value={galleryFilter.text}
                    onChange={handleGalleryFilterChange}
                    placeholder="property value"
                    variant="standard"
                />
        }
        
    }

    return (
        <Grid2 container spacing={1} sx={{ alignItems: 'center', width: '100%' }}>
            <Grid2 size='auto' xs={{ alignSelf: 'center' }}>
                <SelectField
                    name="operator"
                    options={[
                        {
                            label: messages.get(messages.OPERATOR_AND),
                            value: GridFilterOptions.OPERATOR_AND,
                        },
                        {
                            label: messages.get(messages.OPERATOR_OR),
                            value: GridFilterOptions.OPERATOR_OR,
                        },
                    ]}
                    value={galleryFilter.operator}
                    onChange={handleGalleryFilterChange}
                    variant="standard"
                />
            </Grid2>
            <Grid2 size={{ xs: 12, sm: 4 }}>
                <SelectField
                    name="property"
                    options={options}
                    value={galleryFilter.property}
                    onChange={handleGalleryFilterChange}
                    variant="standard"
                />
            </Grid2>
            <Grid2 size='grow'>
                {renderInputFilter()}
            </Grid2>
        </Grid2>
    );
};

export default GalleryFilter;