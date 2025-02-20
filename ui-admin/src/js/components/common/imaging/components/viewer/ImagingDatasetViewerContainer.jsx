import React from 'react';
import { makeStyles } from '@mui/styles';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import ImageSection from "@src/js/components/common/imaging/components/viewer/ImageSection.js";
import MetadataSection from '@src/js/components/common/imaging/components/viewer/MetadataSection.js';
import { useImagingDataContext } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx';
import PreviewSection from '@src/js/components/common/imaging/components/viewer/PreviewSection.jsx';

const useStyles = makeStyles(theme => ({
    container: {
        height: '100%',
        overflow: 'auto',
        padding: '0px 8px'
    },
}));

const ImagingDatasetViewerContainer = () => {
    const classes = useStyles();
    const { state, handleErrorCancel, handleEditComment} = useImagingDataContext();
    const {
        imagingDataset,
        activeImageIdx,
        activePreviewIdx,
        error,
        open,
        loaded,
        imagingTags,
    } = state;
    //console.log('ImagingDatasetViewerContainer - state: ', state);
    if (!loaded) return null;
    const activeImage = imagingDataset.images[activeImageIdx];
    const activePreview = activeImage.previews[activePreviewIdx];
    return (
        <div className={classes.container}>
            <LoadingDialog
                loading={open} />
            <ErrorDialog
                open={error.state}
                error={error.error}
                onClose={handleErrorCancel} />
            <ImageSection
                configExports={activeImage.config.exports} />
            <PreviewSection
                activePreview={activePreview}
                activeImage={activeImage} />
            <MetadataSection
                activePreview={activePreview}
                activeImage={activeImage}
                imagingTags={imagingTags}
                onEditComment={handleEditComment} />
        </div>
    );
};

export default ImagingDatasetViewerContainer;
