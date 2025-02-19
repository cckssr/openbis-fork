function ImagingTechnology() {
    this.init();
}

$.extend(ImagingTechnology.prototype, ELNLIMSPlugin.prototype, {
    init: function () {

    },
    forcedDisableRTF: [],
    forceMonospaceFont: [],
    _getDataListDynamic: function(dataSets) {
        return function(callback, pagOptions) {
            require([ "as/dto/dataset/id/DataSetPermId", "as/dto/dataset/fetchoptions/DataSetFetchOptions" ],
                function(DataSetPermId, DataSetFetchOptions) {
                    var ids = [new DataSetPermId(dataSets[pagOptions.pageIndex].permId.permId)];
                    var fetchOptions = new DataSetFetchOptions();
                    mainController.openbisV3.getDataSets(ids, fetchOptions).done(function(map) {
                        var datasets = Util.mapValuesToList(map);
                        callback(datasets);
                    });
            });
        }
    },
    displayImagingTechViewer: function ($container, isDataset, objId, objType, onActionCallback, objTypeCode) {
        let $element = $("<div>")
        require(["dss/dto/service/id/CustomDssServiceCode",
                "dss/dto/service/CustomDSSServiceExecutionOptions",
                "imaging/dto/ImagingPreviewContainer",
                "imaging/dto/ImagingDataSetExport",
                "imaging/dto/ImagingDataSetMultiExport",
                "imaging/dto/ImagingDataSetPreview",
                "imaging/dto/ImagingDataSetExportConfig",
                "imaging/dto/ImagingExportIncludeOptions",
                "as/dto/property/DataType",
                "as/dto/experiment/fetchoptions/ExperimentFetchOptions",
                "as/dto/experiment/id/ExperimentPermId",
                "as/dto/sample/fetchoptions/SampleFetchOptions",
                "as/dto/sample/id/SamplePermId",
                "as/dto/dataset/search/DataSetSearchCriteria",
                "as/dto/dataset/search/DataSetTypeSearchCriteria",
                "as/dto/vocabulary/search/VocabularySearchCriteria",
                "as/dto/vocabulary/search/VocabularyTermSearchCriteria",
                "as/dto/vocabulary/fetchoptions/VocabularyTermFetchOptions",
                "as/dto/dataset/search/SearchDataSetsOperation",
                "as/dto/dataset/update/DataSetUpdate",
                "as/dto/dataset/id/DataSetPermId",
                "as/dto/dataset/fetchoptions/DataSetFetchOptions",
                "as/dto/dataset/fetchoptions/DataSetTypeFetchOptions",
                "util/Json"],
            function (CustomDssServiceCode, CustomDSSServiceExecutionOptions,
                      ImagingPreviewContainer, ImagingDataSetExport,
                      ImagingDataSetMultiExport, ImagingDataSetPreview,
                      ImagingDataSetExportConfig, ImagingExportIncludeOptions,
                      DataType, ExperimentFetchOptions, ExperimentPermId,
                      SampleFetchOptions, SamplePermId,
                      DataSetSearchCriteria, DataSetTypeSearchCriteria,
                      VocabularySearchCriteria, VocabularyTermSearchCriteria,
                      VocabularyTermFetchOptions,
                      SearchDataSetsOperation, DataSetUpdate, DataSetPermId,
                      DataSetFetchOptions, DataSetTypeFetchOptions,
                      utilJson) {
                let props = {
                    objId: objId,
                    objType: objType,
                    extOpenbis: {
                        CustomDssServiceCode: CustomDssServiceCode,
                        CustomDSSServiceExecutionOptions: CustomDSSServiceExecutionOptions,
                        ImagingPreviewContainer: ImagingPreviewContainer,
                        ImagingDataSetExport: ImagingDataSetExport,
                        ImagingDataSetMultiExport: ImagingDataSetMultiExport,
                        ImagingDataSetPreview: ImagingDataSetPreview,
                        ImagingDataSetExportConfig: ImagingDataSetExportConfig, 
                        ImagingExportIncludeOptions: ImagingExportIncludeOptions,
                        SampleFetchOptions: SampleFetchOptions,
                        SamplePermId: SamplePermId,
                        DataType: DataType,
                        ExperimentFetchOptions: ExperimentFetchOptions,
                        ExperimentPermId: ExperimentPermId,
                        DataSetSearchCriteria: DataSetSearchCriteria,
                        DataSetTypeSearchCriteria: DataSetTypeSearchCriteria,
                        VocabularySearchCriteria: VocabularySearchCriteria,
                        VocabularyTermSearchCriteria: VocabularyTermSearchCriteria,
                        VocabularyTermFetchOptions: VocabularyTermFetchOptions,
                        SearchDataSetsOperation: SearchDataSetsOperation,
                        DataSetUpdate: DataSetUpdate,
                        DataSetPermId: DataSetPermId,
                        DataSetFetchOptions: DataSetFetchOptions,
                        DataSetTypeFetchOptions: DataSetTypeFetchOptions,
                        getDataSets: mainController.openbisV3.getDataSets.bind(mainController.openbisV3),
                        searchDataSets: mainController.openbisV3.searchDataSets.bind(mainController.openbisV3),
                        searchDataSetTypes: mainController.openbisV3.searchDataSetTypes.bind(mainController.openbisV3),
                        searchVocabularyTerms: mainController.openbisV3.searchVocabularyTerms.bind(mainController.openbisV3),
                        updateDataSets: mainController.openbisV3.updateDataSets.bind(mainController.openbisV3),
                        executeCustomDSSService: mainController.openbisV3.getDataStoreFacade().executeCustomDSSService.bind(mainController.openbisV3.getDataStoreFacade()),
                        getExperiments: mainController.openbisV3.getExperiments.bind(mainController.openbisV3),
                        getSamples: mainController.openbisV3.getSamples.bind(mainController.openbisV3),
                        fromJson: utilJson.fromJson.bind(utilJson)
                    }
                }
                let reactImagingComponent = null;
                if (isDataset) {
                    props['onUnsavedChanges'] = onActionCallback
                    reactImagingComponent = React.createElement(window.NgComponents.default.ImagingDatasetViewer, props)
                } else {
                    let configKey = "IMAGING_GALLERY_VIEW-" + objTypeCode;
                    let loadDisplaySettings = function (callback) {
                        mainController.serverFacade.getSetting(configKey, function (config) {
                            callback(config);
                        });
                    }
                    let storeDisplaySettings = function (config, callback) {
                        mainController.serverFacade.setSetting(configKey, config);
                        if (callback) callback();
                    }
                    props['onOpenPreview'] = onActionCallback;
                    props['onStoreDisplaySettings'] = storeDisplaySettings;
                    props['onLoadDisplaySettings'] = loadDisplaySettings;
                    reactImagingComponent = React.createElement(window.NgComponents.default.ImagingGalleryViewer, props)
                }

                NgComponentsManager.renderComponent(reactImagingComponent, $element.get(0));
            }
        );
        $container.append($element);
    },
    experimentFormTop: function ($container, model) {
        if (model.mode === FormMode.VIEW) {
            let isGalleryView = model.experiment &&
                model.experiment.properties["DEFAULT_COLLECTION_VIEW"] &&
                model.experiment.properties["DEFAULT_COLLECTION_VIEW"] === "IMAGING_GALLERY_VIEW";
            if (isGalleryView) {
                var _this = this;
                this.displayImagingTechViewer($container, false, model.experiment.permId, 'collection',
                    function (objId) {
                        var dataSets = model.v3_experiment.dataSets;
                        var paginationInfo = null;
                        var indexFound = null;
                        for(var idx = 0; idx < dataSets.length; idx++) {
                            if(dataSets[idx].permId.permId === objId) {
                                indexFound = idx;
                                break;
                            }
                        }
                        if(indexFound !== null) {
                            paginationInfo = {
                                pagFunction : _this._getDataListDynamic(dataSets),
                                pagOptions : {},
                                currentIndex : indexFound,
                                totalCount : dataSets.length
                            }
                        }
                        var arg = {
                                permIdOrIdentifier : objId,
                                paginationInfo : paginationInfo
                        }
                        mainController.changeView('showViewDataSetPageFromPermId', arg)
                    }, model.experiment.experimentTypeCode);
            }
        }
    },
    sampleFormTop: function ($container, model) {
        if (model.mode === FormMode.VIEW) {
            let isGalleryView = model.sample &&
                model.sample.properties["DEFAULT_OBJECT_VIEW"] &&
                model.sample.properties["DEFAULT_OBJECT_VIEW"] === "IMAGING_GALLERY_VIEW";
            if (isGalleryView) {
                var _this = this;
                this.displayImagingTechViewer($container, false, model.sample.permId, 'object',
                    function (objId) {
                        var dataSets = model.v3_sample.dataSets;
                        var paginationInfo = null;
                        var indexFound = null;
                        for(var idx = 0; idx < dataSets.length; idx++) {
                            if(dataSets[idx].permId.permId === objId) {
                                indexFound = idx;
                                break;
                            }
                        }
                        if(indexFound !== null) {
                            paginationInfo = {
                                pagFunction : _this._getDataListDynamic(dataSets),
                                pagOptions : {},
                                currentIndex : indexFound,
                                totalCount : dataSets.length
                            }
                        }
                        var arg = {
                                permIdOrIdentifier : objId,
                                paginationInfo : paginationInfo
                        }
                        mainController.changeView('showViewDataSetPageFromPermId', arg)
                    }, model.sampleType.code);
            }
        }
    },
    dataSetFormTop: function ($container, model) {
        if (model.mode === FormMode.VIEW) {
            // Potentially any DataSet Type can be an Imaging DataSet Type. The system will know what DataSet Types
            // are an Imaging DataSet by convention, those Types SHOULD end with IMAGING_DATA on their Type Code.
            let isImagingDatasetView = model.dataSetV3 &&
                model.dataSetV3.type.code.endsWith("IMAGING_DATA") &&
                model.dataSetV3.properties["DEFAULT_DATASET_VIEW"] &&
                model.dataSetV3.properties["DEFAULT_DATASET_VIEW"] === "IMAGING_DATASET_VIEWER";
            if (isImagingDatasetView) {
                let viewDirty = function(objId, isDirty) {
                    model.isFormDirty = isDirty;
                }
                this.displayImagingTechViewer($container, true, model.dataSetV3.permId.permId, '', viewDirty, null);
            }
        }
    }
});

profile.plugins.push(new ImagingTechnology());