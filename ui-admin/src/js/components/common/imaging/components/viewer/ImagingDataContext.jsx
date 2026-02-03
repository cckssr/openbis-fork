import React, { useState, useEffect, createContext, useContext, useCallback, useMemo } from 'react';
import { convertToBase64, isObjectEmpty, createInitValues } from "@src/js/components/common/imaging/utils.js";
import constants from '@src/js/components/common/imaging/constants.js';

import ImagingFacade from "@src/js/components/common/imaging/ImagingFacade.js";
import ImagingMapper from '@src/js/components/common/imaging/ImagingMapper.js';

const ImagingDataContext = createContext();

// Custom hook for accessing the context
export const useImagingDataContext = () => useContext(ImagingDataContext);

export const ImagingDataProvider = ({ onUnsavedChanges, objId, objType, extOpenbis, children, showSemanticAnnotations}) => {
    const imagingFacade = useMemo(() => new ImagingFacade(extOpenbis), [extOpenbis]);
    const mapper = useMemo(() => new ImagingMapper(extOpenbis), [extOpenbis]);
    const [state, setState] = useState({
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
        datasetType: '',
        datasetFilePaths: [],
        showSemanticAnnotations: showSemanticAnnotations
    });
    const [autoUpdate, setAutoUpdate] = useState(false);

    const loadImagingDataset = useCallback(async () => {
        if (!state.loaded) {
            try {
                const [datasetFilePaths, datasetType, imagingDataSetPropertyConfig] = await imagingFacade.loadImagingDataset(objId, false, true, true);
                const imagingTagsArr = await imagingFacade.loadImagingVocabularyTerms(constants.IMAGING_TAGS);

                const isInitConfigEmpty = isObjectEmpty(imagingDataSetPropertyConfig.images[0].previews[0].config);
                if (isInitConfigEmpty) {
                    imagingDataSetPropertyConfig.images[0].previews[0].config = createInitValues(imagingDataSetPropertyConfig.images[0].config.inputs, {});
                }
                setState(prev => ({
                    ...prev,
                    open: false,
                    loaded: true,
                    isChanged: isInitConfigEmpty,
                    imagingDataset: imagingDataSetPropertyConfig,
                    imagingTags: imagingTagsArr,
                    datasetType: datasetType,
                    datasetFilePaths: datasetFilePaths
                }));
                //console.log('imagingDataSetPropertyConfig : ', imagingDataSetPropertyConfig);
            } catch (error) {
                handleError(error);
            }
        }
    }, [state.loaded, objId, imagingFacade]);

    const createLocatedSXMPreview = async (sxmPermId, sxmFilePath) => {
        handleOpen();
        const newActivePreviewIdx = createNewPreview();
        const { activeImageIdx, imagingDataset } = state;
        const selectedSpectraPreview = imagingDataset.images[activeImageIdx].previews[newActivePreviewIdx];
        const newSpectraPreview = await imagingFacade.createLocatedSXMPreview(objId, sxmPermId, sxmFilePath, activeImageIdx, selectedSpectraPreview);
        if (newSpectraPreview.error) {
            setState(prev => ({ ...prev, open: false, isChanged: true, isSaved: false }));
            handleError(newSpectraPreview.error);
        }
        deleteSerializationIds(newSpectraPreview);
        let toUpdateImgDs = { ...imagingDataset };
        toUpdateImgDs.images[activeImageIdx].previews[newActivePreviewIdx] = newSpectraPreview.preview;
        setState(prev => ({
            ...prev,
            open: false,
            imagingDataset: toUpdateImgDs,
            isChanged: false,
            isSaved: false
        }));
        if (onUnsavedChanges !== null)
            onUnsavedChanges(objId, true);
    }

    useEffect(() => {
        loadImagingDataset();
    }, []);

    const handleErrorCancel = () => {
        setState(prev => ({
            ...prev,
            error: { open: false, error: null }
        }));
    }

    const handleError = (error) => {
        setState(prev => ({ ...prev, error: { open: true, error } }));
    };

    const handleOpen = () => {
        setState(prev => ({ ...prev, open: true }));
    }

    const saveDataset = async () => {
        const { imagingDataset } = state;
        handleOpen();
        try {
            const isSaved = await imagingFacade.saveImagingDataset(objId, objType, imagingDataset);
            if (isSaved === null) {
                setState(prev => ({ ...prev, open: false, isChanged: false, isSaved: true }));
                if (onUnsavedChanges !== null)
                    onUnsavedChanges(objId, false);
            }
        } catch (error) {
            setState(prev => ({ ...prev, open: false, isChanged: false, isSaved: false }));
            handleError(error);
        }
    };

    const handleOnAddFilter = async (updatedFilterConfig) => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        let toUpdateImgDs = { ...imagingDataset };
        let currentPreview = toUpdateImgDs.images[activeImageIdx].previews[activePreviewIdx]
        currentPreview.filterConfig = updatedFilterConfig;
        setState(prev => ({
            ...prev,
            imagingDataset: toUpdateImgDs,
            isChanged: true
        }));
        if (autoUpdate) {
            handleUpdate();
        }
    }

    const deleteSerializationIds = (imagingDataset) => {
        delete imagingDataset.preview['@id']; //@id are duplicated across different previews on update, need to be deleted
        imagingDataset.preview.filterConfig?.forEach(element => {
            delete element['@id']
        });
    }

    const handleUpdate = async () => {
        handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        try {
            const updatedImagingDataset = await imagingFacade.updateImagingDataset(objId, activeImageIdx, imagingDataset.images[activeImageIdx].previews[activePreviewIdx]);
            if (updatedImagingDataset.error) {
                setState(prev => ({ ...prev, open: false, isChanged: true, isSaved: false }));
                handleError(updatedImagingDataset.error);
            }
            deleteSerializationIds(updatedImagingDataset);
            let toUpdateImgDs = { ...imagingDataset };
            toUpdateImgDs.images[activeImageIdx].previews[activePreviewIdx] = updatedImagingDataset.preview;
            setState(prev => ({
                ...prev,
                open: false,
                imagingDataset: toUpdateImgDs,
                isChanged: false,
                isSaved: false
            }));
            if (onUnsavedChanges !== null)
                onUnsavedChanges(objId, true);
            if (imagingDataset.metadata[constants.GENERATE] && imagingDataset.metadata[constants.GENERATE].toLowerCase() === 'true')
                window.location.reload();
        } catch (error) {
            setState(prev => ({ ...prev, open: false, isChanged: true, isSaved: false }));
            handleError(error);
        }
    };

    const onExport = async (state) => {
        handleOpen();
        const { activeImageIdx } = state;
        try {
            const exportResult = await imagingFacade.exportImagingDataset(objId, activeImageIdx, state, {});
            if (exportResult.url) {
                window.open(exportResult.url, '_blank');
                setState(prev => ({...prev, open: false}));
            } else {
                const error = exportResult.error || 'No URL returned from the server.';
                setState(prev => ({ ...prev, open: false }));
                handleError(error);
            }
        } catch (error) {
            setState(prev => ({ ...prev, open: false }));
            handleError(error);
        }
    };

    const deletePreview = async () => {
        handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        let toUpdateImgDs = { ...imagingDataset };
        toUpdateImgDs.images[activeImageIdx].previews.splice(activePreviewIdx, 1);
        toUpdateImgDs.images[activeImageIdx].previews = toUpdateImgDs.images[activeImageIdx].previews.map(p => {
            if (p.index > activePreviewIdx)
                p.index -= 1;
            return p;
        });
        setState(prev => ({ ...prev, imagingDataset: toUpdateImgDs, activePreviewIdx: 0 }));
        saveDataset();
    };

    const handleUpload = async (file) => {
        handleOpen();
        const base64 = await convertToBase64(file);
        const { imagingDataset, activeImageIdx } = state;
        try {
            let toUpdateImgDs = { ...imagingDataset };
            let newLastIdx = toUpdateImgDs.images[activeImageIdx].previews.length;
            let previewTemplate = mapper.getImagingDataSetPreview(
                {},
                file.type.split('/')[1],
                base64.split(',')[1],
                null,
                null,
                newLastIdx,
                false,
                { 'file': file },
                [],
                '',
                []
            );
            toUpdateImgDs.images[activeImageIdx].previews = [...toUpdateImgDs.images[activeImageIdx].previews, previewTemplate];
            setState(prev => ({ ...prev, open: false, imagingDataset: toUpdateImgDs, isSaved: false }));
            if (onUnsavedChanges !== null)
                onUnsavedChanges(objId, true);
        } catch (error) {
            setState(prev => ({ ...prev, open: false }));
            handleError(error);
        }
    };

    const handleActiveImageChange = (selectedImageIdx) => {
        setState(prev => ({ ...prev, activeImageIdx: selectedImageIdx, activePreviewIdx: 0 }));
    };

    const handleActivePreviewChange = (selectedPreviewIdx) => {
        setState(prev => ({ ...prev, activePreviewIdx: selectedPreviewIdx }));
    };

    const onMove = (position) => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        handleOpen();
        let toUpdateImgDs = { ...imagingDataset };
        let previewsList = toUpdateImgDs.images[activeImageIdx].previews;
        let tempMovedPreview = previewsList[activePreviewIdx];
        tempMovedPreview.index += position;
        previewsList[activePreviewIdx] = previewsList[activePreviewIdx + position];
        previewsList[activePreviewIdx].index -= position;
        previewsList[activePreviewIdx + position] = tempMovedPreview;
        toUpdateImgDs.images[activeImageIdx].previews = previewsList;
        setState(prev => ({ ...prev, open: false, imagingDataset: toUpdateImgDs, isSaved: false }));
        if (onUnsavedChanges !== null)
            onUnsavedChanges(objId, true);
    }

    const handleEditComment = (comment) => {
        handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        let toUpdateImgDs = { ...imagingDataset };
        toUpdateImgDs.images[activeImageIdx].previews[activePreviewIdx].comment = comment;
        setState(prev => ({ ...prev, open: false, imagingDataset: toUpdateImgDs, isSaved: false }));
    }

    const handleTagImage = (tagAll, tags) => {
        handleOpen();
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        let toUpdateImgDs = { ...imagingDataset };
        if (tagAll) {
            toUpdateImgDs.images[activeImageIdx].previews.map(preview => preview.tags = tags)
            setState(prev => ({ ...prev, open: false, imagingDataset: toUpdateImgDs, isSaved: false }));
        } else {
            toUpdateImgDs.images[activeImageIdx].previews[activePreviewIdx].tags = tags;
            setState(prev => ({ ...prev, open: false, imagingDataset: toUpdateImgDs, isSaved: false }));
        }
    }

    const handleResolutionChange = (event) => {
        const v_list = event.target.value.split('x');
        setState(prev => ({ ...prev, resolution: v_list }));
    };

    const handleActiveConfigChange = (name, value, update = false, autoUpdate = false) => {
        console.log('handleActiveConfigChange - name: ', name, 'value: ', value, 'update: ', update);
        const { imagingDataset, activeImageIdx, activePreviewIdx, } = state;
        let toUpdateIDS = { ...imagingDataset };
        toUpdateIDS.images[activeImageIdx].previews[activePreviewIdx].config[name] = value;
        setState(prev => ({ ...prev, imagingDataset: toUpdateIDS, isChanged: true }));
        // Used by the player to autoupdate
        if (update || autoUpdate) {
            handleUpdate();
        }
    }

    const handleShowPreview = () => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        let toUpdateIDS = { ...imagingDataset };
        toUpdateIDS.images[activeImageIdx].previews[activePreviewIdx].show = !toUpdateIDS.images[activeImageIdx].previews[activePreviewIdx].show;
        setState(prev => ({ ...prev, imagingDataset: toUpdateIDS, isSaved: false }));
        if (onUnsavedChanges !== null)
            onUnsavedChanges(objId, true);
    }

    const createNewPreview = () => {
        const { imagingDataset, activeImageIdx, activePreviewIdx } = state;
        let toUpdateImgDs = { ...imagingDataset };
        let activeImage = toUpdateImgDs.images[activeImageIdx];
        let newLastIdx = activeImage.previews.length;
        let inputValues = createInitValues(imagingDataset.images[0].config.inputs, activeImage.previews[activePreviewIdx].config);
        let imagingDataSetPreview = mapper.getImagingDataSetPreview(inputValues, 'png', null, null, null, newLastIdx, false, {}, [], '', []);
        activeImage.previews = [...activeImage.previews, imagingDataSetPreview];
        setState(prev => ({
            ...prev,
            activePreviewIdx: newLastIdx,
            imagingDataset: toUpdateImgDs,
            isChanged: true,
            isSaved: false
        }));
        if (onUnsavedChanges !== null)
            onUnsavedChanges(objId, true);
        return newLastIdx;
    };

    return (
        <ImagingDataContext.Provider value={{
            state, autoUpdate, setAutoUpdate, handleOpen, handleError, handleErrorCancel,
            saveDataset, handleOnAddFilter,
            handleUpdate, onExport, deletePreview,
            handleActiveImageChange, handleActivePreviewChange,
            onMove, handleEditComment, handleTagImage,
            handleResolutionChange, handleActiveConfigChange,
            handleShowPreview, createNewPreview, handleUpload,
            createLocatedSXMPreview
        }}>
            {children}
        </ImagingDataContext.Provider>
    );
};