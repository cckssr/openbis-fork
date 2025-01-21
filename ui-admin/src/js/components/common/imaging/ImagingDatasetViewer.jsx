import React from 'react'
import withStyles from '@mui/styles/withStyles';
import { Divider, Grid2, Typography, useMediaQuery } from '@mui/material';
import { convertToBase64, inRange, isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import Container from '@src/js/components/common/form/Container.jsx'
import ImagingFacade from '@src/js/components/common/imaging/ImagingFacade.js';
import constants from '@src/js/components/common/imaging/constants.js';
import ImagingMapper from '@src/js/components/common/imaging/ImagingMapper.js';
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx';
import ErrorDialog from '@src/js/components/common/error/ErrorDialog.jsx';
import ImageSection from '@src/js/components/common/imaging/components/viewer/ImageSection.js';
import MainPreview from '@src/js/components/common/imaging/components/viewer/MainPreview.js';
import MainPreviewInputControls from '@src/js/components/common/imaging/components/viewer/MainPreviewInputControls.js';
import MetadataSection from '@src/js/components/common/imaging/components/viewer/MetadataSection.js';
import makeStyles from '@mui/styles/makeStyles';

import CustomSwitch from '@src/js/components/common/imaging/components/common/CustomSwitch.jsx';
import RefreshIcon from '@mui/icons-material/Refresh';
import messages from '@src/js/common/messages.js'
import Message from '@src/js/components/common/form/Message.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';

const styles = theme => ({
    container: {
        height: '100%',
        overflow: 'auto'
    },
    gridDirection: {
        [theme.breakpoints.up('md')]: {
            flexDirection: 'row',
        },
        [theme.breakpoints.down('md')]: {
            flexDirection: 'column',
        }
    }
});

class ImagingDataSetViewer extends React.PureComponent {
    constructor(props) {
        super(props)
        this.state = {
            error: { open: false, error: null },
            isSaved: true,
            isChanged: false,
            open: true,
            loaded: false,
            imagingDataset: {},
            activeImageIdx: 0,
            activePreviewIdx: 0,
            resolution: ['original'],
            imagingTags: [],
            datasetType: ''
        }
    }

    async loadImagingDataset() {
        const { objId, extOpenbis } = this.props;
        try {
            const imagingFacade = new ImagingFacade(extOpenbis);
            const [datasetType, imagingDataSetPropertyConfig] = await imagingFacade.loadImagingDataset(objId, false, true);
            const imagingTagsArr = await imagingFacade.loadImagingVocabularyTerms(constants.IMAGING_TAGS);

            const isInitConfigEmpty = isObjectEmpty(imagingDataSetPropertyConfig.images[0].previews[0].config);
            if (isInitConfigEmpty) {
                imagingDataSetPropertyConfig.images[0].previews[0].config = this.createInitValues(imagingDataSetPropertyConfig.images[0].config.inputs, {});
            }
            this.setState({
                open: false,
                loaded: true,
                isChanged: isInitConfigEmpty,
                imagingDataset: imagingDataSetPropertyConfig,
                imagingTags: imagingTagsArr,
                datasetType: datasetType
            });
            console.log('imagingDataSetPropertyConfig : ', imagingDataSetPropertyConfig);
        } catch (error) {
            this.handleError(error);
        }
    }

    async componentDidMount() {
        console.log('componentDidMount : ', this.state);
        if (!this.state.loaded) {
            this.loadImagingDataset();
        }
    }

    saveDataset = async () => {
        const { objId, extOpenbis, onUnsavedChanges } = this.props;
        const { imagingDataset } = this.state;
        this.handleOpen();
        try {
            const isSaved = await new ImagingFacade(extOpenbis).saveImagingDataset(objId, imagingDataset);
            if (isSaved === null) {
                this.setState({ open: false, isChanged: false, isSaved: true });
                if (onUnsavedChanges !== null)
                    onUnsavedChanges(this.props.objId, false);
            }
        } catch (error) {
            this.setState({ open: false, isChanged: false, isSaved: false });
            this.handleError(error);
        }
    }

    handleUpdate = async () => {
        this.handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = this.state;
        const { objId, extOpenbis, onUnsavedChanges } = this.props;
        try {
            const updatedImagingDataset = await new ImagingFacade(extOpenbis)
                .updateImagingDataset(objId, activeImageIdx, imagingDataset.images[activeImageIdx].previews[activePreviewIdx]);
            console.log('handleUpdate - updatedImagingDataset: ', updatedImagingDataset);
            if (updatedImagingDataset.error) {
                this.setState({ open: false, isChanged: true, isSaved: false });
                this.handleError(updatedImagingDataset.error);
            }
            delete updatedImagingDataset.preview['@id']; //@id are duplicated across different previews on update, need to be deleted
            let toUpdateImgDs = { ...imagingDataset };
            toUpdateImgDs.images[activeImageIdx].previews[activePreviewIdx] = updatedImagingDataset.preview;
            this.setState({
                open: false,
                imagingDataset: toUpdateImgDs,
                isChanged: false,
                isSaved: false
            });
            if (onUnsavedChanges !== null)
                onUnsavedChanges(this.props.objId, true);
            if (imagingDataset.metadata[constants.GENERATE] && imagingDataset.metadata[constants.GENERATE].toLowerCase() === 'true') {
                window.location.reload();
            }
        } catch (error) {
            this.setState({ open: false, isChanged: true, isSaved: false });
            this.handleError(error);
        }
    };

    onExport = async (state) => {
        this.handleOpen();
        const { activeImageIdx } = this.state;
        const { objId, extOpenbis } = this.props;
        try {
            const downloadableURL = await new ImagingFacade(extOpenbis)
                .exportImagingDataset(objId, activeImageIdx, state, {});
            if (downloadableURL)
                window.open(downloadableURL, '_blank');
            this.setState({ open: false });
        } catch (error) {
            this.setState({ open: false });
            this.handleError(error);
        }
    };

    handleErrorCancel = () => {
        this.setState({
            error: { open: false, error: null }
        })
    }

    handleError = (error) => {
        this.setState({
            error: { open: true, error: error }
        })
    }

    handleOpen = () => {
        this.setState({ open: true });
    }

    handleActiveImageChange = (selectedImageIdx) => {
        this.setState({ activeImageIdx: selectedImageIdx, activePreviewIdx: 0 });
    };

    handleActivePreviewChange = (selectedPreviewIdx) => {
        this.setState({ activePreviewIdx: selectedPreviewIdx });
    };

    handleResolutionChange = (event) => {
        /*console.log('handleResolutionChange: ', event);
        let val = event.target.value;
        console.log(val);
        if (val === 'original'){
            const {imagingDataset, activeImageIdx, activePreviewIdx,} = this.state;
            const {width, height} = imagingDataset.images[activeImageIdx].previews[activePreviewIdx];
            this.setState({resolution: [width, height]});
        } else if (val.includes('x')){*/
        const v_list = event.target.value.split('x');
        this.setState({ resolution: v_list });
    };

    handleActiveConfigChange = (name, value, update = false) => {
        const { imagingDataset, activeImageIdx, activePreviewIdx, } = this.state;
        let toUpdateIDS = { ...imagingDataset };
        toUpdateIDS.images[activeImageIdx].previews[activePreviewIdx].config[name] = value;
        this.setState({ imagingDataset: toUpdateIDS, isChanged: true });
        // Used by the player to autoupdate
        if (update) {
            this.handleUpdate();
        }
    }

    handleShowPreview = () => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = this.state;
        const { onUnsavedChanges } = this.props;
        let toUpdateIDS = { ...imagingDataset };
        toUpdateIDS.images[activeImageIdx].previews[activePreviewIdx].show = !toUpdateIDS.images[activeImageIdx].previews[activePreviewIdx].show;
        this.setState({ imagingDataset: toUpdateIDS, isSaved: false });
        if (onUnsavedChanges !== null)
            onUnsavedChanges(this.props.objId, true);
    }

    onMove = (position) => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = this.state;
        const { onUnsavedChanges } = this.props;
        this.handleOpen();
        let toUpdateImgDs = { ...imagingDataset };
        let previewsList = toUpdateImgDs.images[activeImageIdx].previews;
        let tempMovedPreview = previewsList[activePreviewIdx];
        tempMovedPreview.index += position;
        previewsList[activePreviewIdx] = previewsList[activePreviewIdx + position];
        previewsList[activePreviewIdx].index -= position;
        previewsList[activePreviewIdx + position] = tempMovedPreview;
        toUpdateImgDs.images[activeImageIdx].previews = previewsList;
        this.setState({ open: false, imagingDataset: toUpdateImgDs, isSaved: false });
        if (onUnsavedChanges !== null)
            onUnsavedChanges(this.props.objId, true);
    }

    createInitValues = (inputsConfig, activeConfig) => {
        const isActiveConfig = isObjectEmpty(activeConfig);
        return Object.fromEntries(inputsConfig.map(input => {
            switch (input.type) {
                case constants.DROPDOWN:
                    if (!isActiveConfig)
                        return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.multiselect ? [input.values[0]] : input.values[0]];
                    else
                        return [input.label, input.multiselect ? [input.values[0]] : input.values[0]];
                case constants.SLIDER:
                    if (!isActiveConfig) {
                        if (input.visibility) {
                            for (const condition of input.visibility) {
                                if (condition.values.includes(activeConfig[condition.label])) {
                                    input.range = condition.range;
                                    input.unit = condition.unit;
                                }
                            }
                        }
                        return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.range[0]];
                    } else {
                        if (input.visibility) {
                            input.range = input.visibility[0].range[0];
                        }
                        return [input.label, input.range[0]];
                    }
                case constants.RANGE:
                    if (!isActiveConfig) {
                        if (input.visibility) {
                            for (const condition of input.visibility) {
                                if (condition.values.includes(activeConfig[condition.label])) {
                                    input.range = condition.range;
                                    input.unit = condition.unit;
                                }
                            }
                        }
                        let rangeInitValue = [inRange(activeConfig[input.label][0], input.range[0], input.range[1]) ? activeConfig[input.label][0] : input.range[0],
                        inRange(activeConfig[input.label][1], input.range[0], input.range[1]) ? activeConfig[input.label][1] : input.range[1]]
                        return [input.label, rangeInitValue];
                    } else {
                        if (input.visibility) {
                            return [input.label, [input.visibility[0].range[0], input.visibility[0].range[1]]];
                        }
                        return [input.label, [input.range[0], input.range[1]]];
                    }
                case constants.COLORMAP:
                    if (!isActiveConfig)
                        return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.values[0]];
                    else
                        return [input.label, input.values[0]];
            }
        }));
    };

    createNewPreview = () => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = this.state;
        const { extOpenbis, onUnsavedChanges } = this.props;
        let toUpdateImgDs = { ...imagingDataset };
        let activeImage = toUpdateImgDs.images[activeImageIdx];
        let newLastIdx = activeImage.previews.length;
        let inputValues = this.createInitValues(imagingDataset.images[0].config.inputs, activeImage.previews[activePreviewIdx].config);
        let imagingDataSetPreview = new ImagingMapper(extOpenbis).getImagingDataSetPreview(inputValues, 'png', null, null, null, newLastIdx, false, {}, [], '');
        activeImage.previews = [...activeImage.previews, imagingDataSetPreview];
        this.setState({
            activePreviewIdx: newLastIdx,
            imagingDataset: toUpdateImgDs,
            isChanged: true,
            isSaved: false
        })
        if (onUnsavedChanges !== null)
            onUnsavedChanges(this.props.objId, true);
    };

    handleUpload = async (file) => {
        this.handleOpen();
        const base64 = await convertToBase64(file);
        const { imagingDataset, activeImageIdx } = this.state;
        const { onUnsavedChanges } = this.props;
        try {
            let toUpdateImgDs = { ...imagingDataset };
            let newLastIdx = toUpdateImgDs.images[activeImageIdx].previews.length;
            let previewTemplate = new ImagingMapper(this.props.extOpenbis).getImagingDataSetPreview(
                {},
                file.type.split('/')[1],
                base64.split(',')[1],
                null,
                null,
                newLastIdx,
                false,
                { 'file': file },
                [],
                ''
            )
            toUpdateImgDs.images[activeImageIdx].previews = [...toUpdateImgDs.images[activeImageIdx].previews, previewTemplate];
            this.setState({ open: false, imagingDataset: toUpdateImgDs, isSaved: false })
            if (onUnsavedChanges !== null)
                onUnsavedChanges(this.props.objId, true);
        } catch (error) {
            this.setState({ open: false });
            this.handleError(error);
        }
    };

    handleTagImage = (tagAll, tags) => {
        this.handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = this.state;
        let toUpdateImgDs = { ...imagingDataset };
        if (tagAll) {
            toUpdateImgDs.images[activeImageIdx].previews.map(preview => preview.tags = tags)
            this.setState({ open: false, imagingDataset: toUpdateImgDs, isSaved: false });
        } else {
            toUpdateImgDs.images[activeImageIdx].previews[activePreviewIdx].tags = tags;
            this.setState({ open: false, imagingDataset: toUpdateImgDs, isSaved: false });
        }
    }

    deletePreview = () => {
        this.handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = this.state;
        let toUpdateImgDs = { ...imagingDataset };
        toUpdateImgDs.images[activeImageIdx].previews.splice(activePreviewIdx, 1);
        toUpdateImgDs.images[activeImageIdx].previews = toUpdateImgDs.images[activeImageIdx].previews.map(p => {
            if (p.index > activePreviewIdx)
                p.index -= 1;
            return p;
        });
        this.setState({ imagingDataset: toUpdateImgDs, activePreviewIdx: 0 });
        this.saveDataset();
    };

    renderPreviewChanges = (isSaved, isChanged, isUploadedPreview) => {
        const warnings = []
        warnings.push(!isSaved && (
            <Message type='warning'>
                {messages.get(messages.UNSAVED_CHANGES)}
            </Message>
        ))
        /* warnings.push(isChanged && !isUploadedPreview && (
                            <Message type='info'>
                                {messages.get(messages.UPDATE_CHANGES)}
                            </Message>
                        )) */
        return warnings
    }

    render() {
        const { loaded, open, error } = this.state;
        if (!loaded) return null;
        const {
            imagingDataset,
            activeImageIdx,
            activePreviewIdx,
            resolution,
            isSaved,
            isChanged,
            imagingTags,
            datasetType
        } = this.state;
        const { classes } = this.props;
        const activeImage = imagingDataset.images[activeImageIdx];
        const activePreview = activeImage.previews[activePreviewIdx];
        console.log('ImagingDataSetViewer.render: ', datasetType, this.state);
        const currentMetadata = activePreview.metadata;
        const isUploadedPreview = datasetType === constants.USER_DEFINED_IMAGING_DATA ? true : isObjectEmpty(currentMetadata) ? false : ('file' in currentMetadata);
        return (
            <Container className={classes.container}>
                <LoadingDialog loading={open} />
                <ErrorDialog open={error.state} error={error.error}
                    onClose={this.handleErrorCancel} />
                <ImageSection images={imagingDataset.images}
                    activeImageIdx={activeImageIdx}
                    configExports={activeImage.config.exports}
                    onActiveItemChange={this.handleActiveImageChange}
                    handleExport={this.onExport}
                />
                <CollapsableSection title='Preview' isCollapsed={false} renderWarnings={this.renderPreviewChanges(isSaved, isChanged, isUploadedPreview)}>
                    <Grid2 container size={{ xs: 12 }} className={classes.gridDirection}>
                        <MainPreview activePreview={activePreview} resolution={resolution} isChanged={isChanged} isUploadedPreview={isUploadedPreview} previews={activeImage.previews}
                            activeImageIdx={activeImageIdx}
                            activePreviewIdx={activePreviewIdx}
                            isSaved={isSaved}
                            onActiveItemChange={this.handleActivePreviewChange}
                            onMove={this.onMove}
                            onClickSave={this.saveDataset}
                            onHandleYes={this.deletePreview}
                            onClickNew={this.createNewPreview}
                            onInputFile={this.handleUpload} />
                        <MainPreviewInputControls activePreview={activePreview}
                            configInputs={activeImage.config.inputs}
                            isUserGenerated={imagingDataset.metadata[constants.GENERATE] && imagingDataset.metadata[constants.GENERATE].toLowerCase() === 'true'}
                            configResolutions={activeImage.config.resolutions}
                            resolution={resolution}
                            isChanged={isChanged}
                            onClickUpdate={this.handleUpdate}
                            onChangeShow={this.handleShowPreview}
                            onSelectChangeRes={this.handleResolutionChange}
                            onChangeActConf={this.handleActiveConfigChange}
                            imagingTags={imagingTags}
                            handleTagImage={this.handleTagImage}
                            datasetType={datasetType}
                        />
                    </Grid2>
                </CollapsableSection>
                <CollapsableSection title='Metadata' isCollapsed={false}>
                    <MetadataSection activePreview={activePreview} activeImage={activeImage} imagingTags={imagingTags} />
                </CollapsableSection>
            </Container>
        )
    };
}

export default withStyles(styles)(ImagingDataSetViewer);
