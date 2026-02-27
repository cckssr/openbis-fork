import * as React from 'react';
import {
    CardActionArea,
    CardActions,
    CardMedia,
    ImageList,
    ImageListItem,
    Grid2,
    Checkbox,
    FormControlLabel
} from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';
import constants from '@src/js/components/common/imaging/constants.js';
import CustomSwitch from '@src/js/components/common/imaging/components/common/CustomSwitch.jsx';

const useStyles = makeStyles((theme) => ({
    content: {
        flex: '1 0 auto',
        alignSelf: 'center',
        justifyContent: 'space-evenly'
    }
}));

const GalleryGridView = ({
    previewContainerList,
    cols,
    selectAll,
    onOpenPreview,
    handleShowPreview,
    handleSelectPreview
}) => {
    const classes = useStyles();

    return (
        <ImageList sx={{ width: '100%', overflow: 'visible' }} cols={cols} >
            {previewContainerList.map((previewContainer, idx) => (
                <ImageListItem sx={{ height: 'unset', justifyContent: 'space-between' }} key={`image-grid-item-${idx}`}>
                    <CardActionArea sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                        <CardMedia component='img'
                            sx={{ height: '200px', width: 'auto' }}
                            alt={''}
                            src={previewContainer.preview.bytes ? `data:image/${previewContainer.preview.format};base64,${previewContainer.preview.bytes}` : constants.BLANK_IMG_SRC}
                            onClick={() => onOpenPreview(previewContainer.datasetId, previewContainer.imageIdx, previewContainer.previewIdx)}
                        />
                    </CardActionArea>
                    {selectAll && <CardActions className={classes.content}>
                        <Grid2 container>
                            <Grid2 sx={{ alignContent: 'center' }}>
                                <CustomSwitch
                                    size='small'
                                    label='Show'
                                    labelPlacement='start'
                                    isChecked={previewContainer.preview.show}
                                    onChange={() => handleShowPreview(previewContainer)} />
                            </Grid2>
                            {previewContainer.preview.bytes &&
                                <Grid2 sx={{ alignContent: 'center' }}>
                                    <FormControlLabel
                                        value='start'
                                        control={<Checkbox checked={previewContainer.select}
                                                            onChange={() => handleSelectPreview(previewContainer)}
                                                            color='primary' />}
                                        label='Select'
                                        labelPlacement='start' />
                                </Grid2>
                            }
                        </Grid2>
                    </CardActions>}
                </ImageListItem>
            ))}
        </ImageList>
    );
}

export default GalleryGridView;
