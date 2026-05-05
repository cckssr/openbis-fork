import React from 'react';
import { Typography, Grid2 } from "@mui/material";

import ImagingFacade from "@src/js/components/common/imaging/ImagingFacade.js";
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import Loading from "@src/js/components/common/loading/Loading.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import Container from '@src/js/components/common/form/Container.jsx'

import GalleryGridView from "@src/js/components/common/imaging/components/gallery/GalleryGridView.js";
import GalleryListView from "@src/js/components/common/imaging/components/gallery/GalleryListView.js";
import GalleryControlsBar from '@src/js/components/common/imaging/components/gallery/GalleryControlsBar.js';

import { loadGalleryViewFilters, loadPreviewsInfo, loadImagingVocabularyTerms } from '@src/js/components/common/imaging/dataHandlers.js'
import constants from '@src/js/components/common/imaging/constants.js';

const ImagingGalleryViewer = ({ objId, objType, extOpenbis, onOpenPreview, onStoreDisplaySettings = null, onLoadDisplaySettings = null }) => {
    const imagingFacade = React.useMemo(() => new ImagingFacade(extOpenbis), [extOpenbis]);

    const [gridView, setGridView] = React.useState(true);
    const [isLoaded, setIsLoaded] = React.useState(false);
    const [isLoading, setIsLoading] = React.useState(false);
    const [open, setOpen] = React.useState(false);
    const [error, setError] = React.useState({ open: false, error: null });
    const [previewsInfo, setPreviewsInfo] = React.useState({
        previewContainerList: [],
        allContainerList: [],
        totalCount: 0
    });
    const [paging, setPaging] = React.useState({ page: 0, pageSize: 10, pageColumns: 4 });
    const [showAll, setShowAll] = React.useState(true);
    const [selectAll, setSelectAll] = React.useState(true);
    const [galleryFilter, setGalleryFilter] = React.useState({
        operator: 'AND',
        text: '',
        property: constants.IMAGING_TAGS
    });
    const [dataSetTypes, setDataSetTypes] = React.useState([]);
    const [imagingTags, setImagingTags] = React.useState([]);

    const getPreviewKey = (previewContainer) => {
        return `${previewContainer.datasetId}-${previewContainer.previewIdx}`;
    }

    const mergeSelections = (previewContainerList, allContainerList) => {
        if (!allContainerList || allContainerList.length === 0) {
            return previewContainerList;
        }
        const selectedByKey = new Map(
            allContainerList.map((container) => [getPreviewKey(container), container.select])
        );
        return previewContainerList.map((container) => {
            const key = getPreviewKey(container);
            if (!selectedByKey.has(key)) {
                return container;
            }
            return { ...container, select: selectedByKey.get(key) };
        });
    }

    React.useEffect(() => {
        // Set the config for the gallery view from previous store config in ELN-LIMS
        if (onLoadDisplaySettings !== null) {
            const setDisplaySettings = (config) => {
                if (config) {
                    const objConfig = JSON.parse(config);
                    setPaging(objConfig.paging);
                    setShowAll(objConfig.showAll);
                    setSelectAll(objConfig.selectAll);
                }
            }
            onLoadDisplaySettings(setDisplaySettings)
        }
        loadGalleryViewFilters(imagingFacade, setDataSetTypes);
        loadImagingVocabularyTerms(imagingFacade, setImagingTags);
    }, [])

    React.useEffect(() => {
        if (onStoreDisplaySettings !== null) {
            onStoreDisplaySettings(JSON.stringify({ paging: paging, showAll: showAll, selectAll: selectAll }), null)
        }
    }, [paging, showAll, selectAll])

    React.useEffect(() => {
        let isCancelled = false;

        const loadGalleryView = async () => {
            const { previewContainerList, totalCount } = await loadPreviewsInfo(imagingFacade, objId, objType, galleryFilter, paging, setOpen);
            if (!isCancelled) {
                setPreviewsInfo((prev) => ({
                    ...prev,
                    previewContainerList: mergeSelections(previewContainerList, prev.allContainerList),
                    totalCount
                }));
                setIsLoaded(true);
                setOpen(false);
                setIsLoading(false);
            }
        };

        loadGalleryView();
        //Cleanup function is called when useEffect is called again or on unmount
        return () => { isCancelled = true; setIsLoading(true) };
    }, [paging.page, paging.pageSize, galleryFilter])

    const handleErrorCancel = () => {
        setError({ open: false, error: null });
    }

    const handleError = (error) => {
        setError({ open: true, error: error });
    }

    const handleOpen = () => {
        setOpen(true);
    }

    const handleViewChange = (isGridView) => {
        if (gridView !== isGridView) setGridView(isGridView);
    }

    const handleSelectAll = (val) => {
        //console.log('handleSelectAll: ', val, previewContainerList.map(previewContainer => previewContainer.select === false));
        if (!val) {
            setPreviewsInfo((prev) => ({
                ...prev,
                previewContainerList: prev.previewContainerList.map((previewContainer) => ({
                    ...previewContainer,
                    select: false
                })),
                allContainerList: []
            }));
        }
        setSelectAll(val);
    }

    const handleShowPreview = async (previewContainer) => {
        handleOpen();
        let selectedPreview = previewContainer.preview;
        selectedPreview.show = !selectedPreview.show;
        try {
            await imagingFacade.updatePreview(previewContainer.objectType,
                previewContainer.datasetId, previewContainer.imageIdx, selectedPreview);
        } catch (error) {
            handleError(error);
        } finally {
            setOpen(false);
        }
    }

    const handleSelectPreview = (previewContainer) => {
        const key = getPreviewKey(previewContainer);
        const nextSelect = !previewContainer.select;
        setPreviewsInfo((prev) => {
            const updatedPreviewContainerList = prev.previewContainerList.map((container) => {
                if (getPreviewKey(container) !== key) {
                    return container;
                }
                return { ...container, select: nextSelect };
            });

            let updatedAllContainerList = prev.allContainerList;
            const existingIndex = prev.allContainerList.findIndex((container) => getPreviewKey(container) === key);
            if (nextSelect) {
                const selectedContainer = { ...previewContainer, select: true };
                updatedAllContainerList = existingIndex === -1
                    ? [...prev.allContainerList, selectedContainer]
                    : prev.allContainerList.map((container, idx) => idx === existingIndex ? selectedContainer : container);
            } else if (existingIndex !== -1) {
                updatedAllContainerList = prev.allContainerList.filter((_, idx) => idx !== existingIndex);
            }

            return {
                ...prev,
                previewContainerList: updatedPreviewContainerList,
                allContainerList: updatedAllContainerList
            };
        });
    }

    const handleExport = async (currentConfigExport) => {
        setOpen(true);
        const exportList = previewsInfo.allContainerList.length > 0
            ? previewsInfo.allContainerList
            : previewsInfo.previewContainerList.filter(previewObj => previewObj.select);
        try {
            const exportResult = await imagingFacade.multiExportImagingDataset(currentConfigExport, exportList);
            if (exportResult.url) {
                window.open(exportResult.url, '_blank');
                setOpen(false);
            } else {
                const error = exportResult.error || 'No URL returned from the server.';
                setOpen(false);
                handleError(error);
            }
        } catch (error) {
            setOpen(false);
            handleError(error);
        }
    }

    const handleEditComment = async (comment, previewContainer, idx) => {
        handleOpen();
        let selectedPreviewContainer = previewContainer;
        selectedPreviewContainer.preview.comment = comment;
        try {
            const isSaved = await imagingFacade.updatePreview(previewContainer.objectType,
                previewContainer.datasetId, previewContainer.imageIdx,
                selectedPreviewContainer.preview);
            if (isSaved === null) {
                let updatedContainerList = [...previewsInfo.previewContainerList];
                updatedContainerList[idx] = selectedPreviewContainer;
                setPreviewsInfo({ ...previewsInfo, previewContainerList: updatedContainerList });
            }
        } catch (error) {
            handleError(error);
        } finally {
            setOpen(false);
        }
    }

    const handleEditNote = async (note, datasetId) => {
        handleOpen();
        try {
            await imagingFacade.editImagingDatasetNote(datasetId, note);
        } catch (error) {
            handleError(error);
        } finally {
            setOpen(false);
        }
    }

    const onGalleryFilterChange = (newGalleryFilter) => {
        setGalleryFilter(newGalleryFilter);
        const newPaging = paging;
        newPaging.page = 0;
        setPaging(newPaging)
    }

    const extractCommonExportsConfig = () => {
        const commonConfig = [];
        previewsInfo.previewContainerList.flatMap(previewContainer => previewContainer.exportConfig)
            .map(exportConfig => {
                let evalIdx = commonConfig.findIndex(x => x.label === exportConfig.label);
                if (evalIdx === -1) {
                    commonConfig.push(exportConfig);
                } else {
                    commonConfig[evalIdx].values = commonConfig[evalIdx].values.filter(value => exportConfig.values.includes(value));
                }
            });
        //console.log('exports - loadedExportConfig: ', commonConfig);
        return commonConfig
    }

    const renderControlsBar = (isExportDisable, configExports) => {
        return <Container>
            <GalleryControlsBar isExportDisable={isExportDisable}
                configExports={configExports}
                gridView={gridView}
                handleViewChange={handleViewChange}
                paging={paging}
                setPaging={setPaging}
                showAll={showAll}
                setShowAll={setShowAll}
                selectAll={selectAll}
                handleSelectAll={handleSelectAll}
                galleryFilter={galleryFilter}
                onGalleryFilterChange={onGalleryFilterChange}
                count={previewsInfo.totalCount}
                handleExport={handleExport}
                dataSetTypes={dataSetTypes}
            />
        </Container>
    }

    if (!isLoaded) return null;
    //console.log("RENDER.ImagingGalleryViewer - previewsInfo: ", previewsInfo);
    const previewContainerList = showAll
        ? previewsInfo.previewContainerList
        : previewsInfo.previewContainerList.filter(previewContainer => previewContainer.preview.show);

    const isExportDisable = !((previewsInfo.allContainerList.length > 0 ||
        previewContainerList.some(previewContainer => previewContainer.select)) && gridView);
    const commonExportConfig = extractCommonExportsConfig();
    return (
        <>
            <LoadingDialog loading={open} />
            <ErrorDialog open={error.state} error={error.error} onClose={handleErrorCancel} />
            {renderControlsBar(isExportDisable, commonExportConfig)}
            <Loading loading={isLoading}>
                {gridView ? (
                    <GalleryGridView
                        previewContainerList={previewContainerList}
                        cols={paging.pageColumns}
                        selectAll={selectAll}
                        onOpenPreview={onOpenPreview}
                        handleShowPreview={handleShowPreview}
                        handleSelectPreview={handleSelectPreview}
                        isLoading={isLoading} />
                ) : (
                    <GalleryListView
                        previewContainerList={previewContainerList}
                        onOpenPreview={onOpenPreview}
                        onEditComment={handleEditComment}
                        onEditNote={handleEditNote}
                        imagingTags={imagingTags} />
                )}
                {previewContainerList.length === 0 && !isLoading && ( // Conditional rendering for empty state
                    <Grid2 container sx={{ justifyContent: "space-evenly" }}>
                        <Typography key="no-dataset-comment" gutterBottom >
                            No Imaging Datasets to display.
                        </Typography>
                    </Grid2>
                )}</Loading>
        </>
    );
}

export default ImagingGalleryViewer;
