import React from "react";
import {
    CardActionArea,
    CardContent,
    CardMedia,
    ImageList,
    ImageListItem,
    Typography,
    Card, Divider,
    Chip
} from "@mui/material";
import TextField from '@src/js/components/common/form/TextField.jsx'
import makeStyles from '@mui/styles/makeStyles';
import constants from "@src/js/components/common/imaging/constants.js";
import { isObjectEmpty } from "@src/js/components/common/imaging/utils.js";
import DefaultMetadataField from "@src/js/components/common/imaging/components/gallery/DefaultMetadataField.js";
import EditableMetadataField from "@src/js/components/common/imaging/components/gallery/EditableMetadataField.jsx";

const useStyles = makeStyles((theme) => ({
    card: {
        marginBottom: '5px',
        display: 'flex',
        boxShadow: 'none',
        [theme.breakpoints.up('md')]: {
            flexDirection: 'row',
            height: '400px'
        },
        [theme.breakpoints.down('md')]: {
            flexDirection: 'column',
        },
    },
    content: {
        flex: '1 0 auto',
        alignSelf: 'center',
    },
    imgFixedWidth: {
        height: '250px',
    },
    field: {
        paddingBottom: theme.spacing(1)
    }
}));


const GalleryListView = ({ previewContainerList, onOpenPreview, onEditComment, onEditNote, imagingTags }) => {
    const classes = useStyles();

    const renderDatasetProps = (datasetProperties, datasetId, idx) => {
        if (isObjectEmpty(datasetProperties)) {
            <p>No Properties to display</p>
        } else {
            return Object.entries(datasetProperties).map(([key, value], pos) => {
                if (key === constants.IMAGING_NOTES) {
                    return <EditableMetadataField keyProp={key}
                        valueProp={value}
                        idx={idx}
                        onEdit={newVal => onEditNote(newVal, datasetId)} />
                } else {
                    return <DefaultMetadataField key={'property-' + idx + '-' + pos} label={key} value={value} />
                }
            })
        }

    }

    const renderTags = (tags, idx) => {
        var trasformedTags = []
        for (const activePreviewTag of tags) {
            const matchTag = imagingTags.find(imagingTag => imagingTag.value === activePreviewTag);
            trasformedTags.push(matchTag.label);
        }
        return <DefaultMetadataField key={'property-tags-' + idx}
            label={'Preview Tags'}
            value={trasformedTags.map(item => (<Chip sx={{ mr: '4px' }} key={item}
                size='small'
                tabIndex={-1}
                label={item} />))} />
    }

    const renderCommentField = (previewContainer, idx) => {
        return <EditableMetadataField keyProp={"Preview Comment"}
            valueProp={previewContainer.preview.comment}
            idx={idx}
            onEdit={newVal => onEditComment(newVal, previewContainer, idx)} />

    }

    const renderMetadataFields = (metadata, idx) => {
        return !isObjectEmpty(metadata) &&
            Object.entries(metadata).map(([key, value], pos) => (<DefaultMetadataField key={'preview-property-' + pos} label={'(Raw metadata) ' + key} value={value} />))
    }

    return (<ImageList sx={{ width: '100%', height: '800px' }} cols={1}>
        {previewContainerList.map((previewContainer, idx) => (
            <ImageListItem style={{ height: 'unset' }} key={'image-list-item-' + idx}>
                <Card className={classes.card} key={'card-list-item-' + idx}>
                    <CardActionArea sx={{ overflow: 'auto', width: { md: '40%', xs: '100%' } }}>
                        <CardMedia component="img"
                            alt={""}
                            src={previewContainer.preview.bytes ? `data:image/${previewContainer.preview.format};base64,${previewContainer.preview.bytes}` : constants.BLANK_IMG_SRC}
                            onClick={() => onOpenPreview(previewContainer.datasetId)}
                        />
                    </CardActionArea>
                    <CardContent className={classes.content}>
                        <Typography key={`dataset-types-header-${idx}`} gutterBottom variant="h6">
                            Data Set Types
                        </Typography>
                        <Typography key={`dataset-types-${idx}`} variant="body2" component={'span'} sx={{ color: "textSecondary" }}>
                            {renderDatasetProps(previewContainer.datasetProperties, previewContainer.datasetId, idx)}
                        </Typography>
                        <Typography key={`preview-metadata-header-${idx}`} gutterBottom variant="h6">
                            Preview Metadata
                        </Typography>
                        <Typography key={`preview-metadata-${idx}`} variant="body2"
                            component={'span'} sx={{
                                color: "textSecondary"
                            }}>
                            {renderMetadataFields(previewContainer.preview.metadata, idx)}
                            {previewContainer.preview.tags !== null && previewContainer.preview.tags.length > 0 && renderTags(previewContainer.preview.tags, idx)}
                            {renderCommentField(previewContainer, idx)}
                        </Typography>
                    </CardContent>
                </Card>
            </ImageListItem>
        ))}
    </ImageList>
    );
}

export default GalleryListView;