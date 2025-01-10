import React from "react";
import { ImageList, ImageListItem, Typography, Divider } from "@mui/material";
import BlankImage from "@src/js/components/common/imaging/components/common/BlankImage.js";
import makeStyles from '@mui/styles/makeStyles';
import ImageListItemBarAction
    from "@src/js/components/common/imaging/components/common/ImageListItemBarAction.js";
import constants from "@src/js/components/common/imaging/constants.js";

const useStyles = makeStyles(() => ({
    imgFullWidth: {
        flexGrow: 'unset !important'
    },
    elevation: {
        boxShadow: '0 3px 10px rgb(0 0 0 / 0.2)',
        border: '3px solid #039be5',
        width: '150px !important',
    },
    trasparency: {
        opacity: 0.5,
        width: '150px !important',
    },
    imageList: {
        flexWrap: 'nowrap',
        // Promote the list into his own layer on Chrome. This cost memory but helps keeping high FPS.
        transform: 'translateZ(0)',
        height: 'auto',
    }
}));

const ImageListItemSection = ({ title, cols, rowHeight, type, items, activeImageIdx, activePreviewIdx, onActiveItemChange, onMove }) => {
    const classes = useStyles();

    const moveArrowCompList = (currentIdx) => {
        let previewsLength = items.length;
        if (currentIdx === 0 && previewsLength === 1) { // only 1 element
            return [];
        } else if (currentIdx === 0) { // first element
            return [<ImageListItemBarAction key={"ImageListItemBarAction-left-" + currentIdx}
                classNames={'singleActionBar'} position={'right'}
                onMove={() => onMove(1)} />];
        } else if (currentIdx === previewsLength - 1) { // last element
            return [<ImageListItemBarAction key={"ImageListItemBarAction-right-" + currentIdx}
                classNames={'singleActionBar'} position={'left'}
                onMove={() => onMove(-1)} />];
        } else {
            return [<ImageListItemBarAction key={"ImageListItemBarAction-left-" + currentIdx}
                classNames={'actionBarL'} position={'left'}
                onMove={() => onMove(-1)} />,
            <ImageListItemBarAction key={"ImageListItemBarAction-right-" + currentIdx}
                classNames={'actionBarR'} position={'right'}
                onMove={() => onMove(1)} />];
        }
    };

    const renderImageListItems = () => {
        switch (type) {
            case constants.IMAGE_TYPE:
                return items.map(image => (
                    <ImageListItem key={`imageListItem-image-${image.index}`}
                        className={activeImageIdx === image.index ? classes.elevation : classes.trasparency}
                        onClick={() => onActiveItemChange(image.index)}>
                        {image.previews[0].bytes ?
                            <img alt={""} className={classes.imgFullWidth} src={`data:image/${image.previews[0].format};base64,${image.previews[0].bytes}`} />
                            : <BlankImage className={classes.imgFullWidth} />}
                    </ImageListItem>
                ))
            case constants.PREVIEW_TYPE:
                return items.map(preview => (
                    <ImageListItem key={`imageListItem-preview-${activeImageIdx}-${preview.index}`}
                        className={activePreviewIdx === preview.index ? classes.elevation : classes.trasparency}
                        onClick={() => onActiveItemChange(preview.index)}>
                        {preview.bytes ?
                            <img alt={""} className={classes.imgFullWidth} src={`data:image/${preview.format};base64,${preview.bytes}`} />
                            : <BlankImage className={classes.imgFullWidth} />}
                        {activePreviewIdx === preview.index && moveArrowCompList(activePreviewIdx)}
                    </ImageListItem>
                ))
            default:
                return null;
        }
    }

    return <>
        {title && <Typography variant='h6'>
            {title}
        </Typography>}
        <ImageList className={classes.imageList}
            cols={cols}
            rowHeight={rowHeight}>
            {renderImageListItems()}
        </ImageList>
    </>
}

export default ImageListItemSection;