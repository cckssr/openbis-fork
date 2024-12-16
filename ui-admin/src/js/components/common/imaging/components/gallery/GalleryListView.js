import React from "react";
import {
    CardActionArea,
    CardContent,
    CardMedia,
    ImageList,
    ImageListItem,
    Typography,
    Card, Divider
} from "@mui/material";
import makeStyles from '@mui/styles/makeStyles';
import constants from "@src/js/components/common/imaging/constants.js";
import { isObjectEmpty } from "@src/js/components/common/imaging/utils.js";
import DefaultMetadataField from "@src/js/components/common/imaging/components/gallery/DefaultMetadaField.js";
import EditableMetadataField from "@src/js/components/common/imaging/components/gallery/EditableMetadataField.jsx";

const useStyles = makeStyles((theme) => ({
    card: {
        margin: '5px',
        display: 'flex',
        flexDirection: 'row',
    },
    content: {
        flex: '1 0 auto',
        alignSelf: 'center',
    },
    imgFixedWidth: {
        height: '250px',
    }
}));

const GalleryListView = ({ previewContainerList, onOpenPreview, onEditComment, onEditNote }) => {
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
                    return <DefaultMetadataField key={'property-' + idx + '-' + pos} keyProp={key} valueProp={value} idx={idx} pos={pos} />
                }
            })
        }

    }

    const renderTags = (tags, idx) => {        
        return tags.length > 0 && <DefaultMetadataField key={'property-tags-' + idx} keyProp={'TAGS'} valueProp={tags} />
    }

    const renderCommentField = (previewContainer, idx) => {
        return <EditableMetadataField keyProp={"COMMENT"}
            valueProp={previewContainer.preview.comment}
            idx={idx}
            onEdit={newVal => onEditComment(newVal, previewContainer, idx)} />

    }

    const renderMetadataFields = (metadata, idx) => {
        return <DefaultMetadataField key={'property-metadata'} keyProp={'METADATA'} valueProp={metadata} />
        /* if (isObjectEmpty(metadata)) {
            return <DefaultMetadataField key={'property-metadata'} keyProp={'METADATA'} valueProp={metadata} />
        } else {
            return Object.entries(metadata).map(([key, value], pos) =>
                <DefaultMetadataField key={'property-' + idx + '-' + pos} keyProp={key} valueProp={value} idx={idx} pos={pos} />)
        } */
    }

    return (<ImageList sx={{ width: '100%', height: '800px' }} cols={1} gap={5}>
        {previewContainerList.map((previewContainer, idx) => (
            <ImageListItem style={{ height: 'unset' }} key={'image-list-item-' + idx}>
                <Card className={classes.card} key={'card-list-item-' + idx}>
                    <CardActionArea style={{ width: 'unset' }}>
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
                        <Divider />
                        <Typography key={`preview-metadata-header-${idx}`} gutterBottom variant="h6">
                            Preview Metadata
                        </Typography>
                        <Typography key={`preview-metadata-${idx}`} variant="body2"
                            component={'span'} sx={{
                                color: "textSecondary"
                            }}>
                            {renderMetadataFields(previewContainer.preview.metadata, idx)}
                            {renderCommentField(previewContainer, idx)}
                            {previewContainer.preview.tags !== null && renderTags(previewContainer.preview.tags, idx)}
                        </Typography>
                    </CardContent>
                </Card>
            </ImageListItem>
        ))}
    </ImageList>
    );
}

export default GalleryListView;