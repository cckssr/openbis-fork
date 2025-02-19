import constants from '@src/js/components/common/imaging/constants.js';
import ImagingMapper from "@src/js/components/common/imaging/ImagingMapper";
import messages from "@src/js/common/messages";
import ObjectType from "@src/js/common/consts/objectType";

const SUPPORTED_DATA_TYPE = ["VARCHAR", "MULTILINE_VARCHAR", "CONTROLLEDVOCABULARY"]; //"JSON"

export default class ImagingFacade {

    constructor(extOpenbis) {
        this.openbis = extOpenbis;
    }

    async loadImagingVocabularyTerms(code) {
        const criteria = new this.openbis.VocabularyTermSearchCriteria();
        criteria.withVocabulary().withCode().thatContains(code);

        const fo = new this.openbis.VocabularyTermFetchOptions();
        fo.sortBy().code().asc();

        const result = await this.openbis.searchVocabularyTerms(criteria, fo);

        return result.getObjects().map(vocabularyTerm => ({ label: vocabularyTerm.label, value: vocabularyTerm.code }));
    }

    async loadDataSetTypes() {
        const fetchOptions = new this.openbis.DataSetTypeFetchOptions();
        fetchOptions.withPropertyAssignments().withPropertyType();

        const result = await this.openbis.searchDataSetTypes(
            new this.openbis.DataSetTypeSearchCriteria(),
            fetchOptions
        )
        //console.log('loadDataSetTypes - result: ', result);
        const dataSetTypesMap = new Map();

        for (const dataSetType of result.getObjects()) {
            if (![constants.IMAGING_DATA, constants.USER_DEFINED_IMAGING_DATA].includes(dataSetType.code)) continue;

            for (const assignment of dataSetType.propertyAssignments) {
                if (!SUPPORTED_DATA_TYPE.includes(assignment.propertyType.dataType)) continue;

                const { code, label, dataType } = assignment.propertyType;

                if (![constants.IMAGING_DATA_CONFIG, constants.DEFAULT_DATASET_VIEW].includes(code)) {
                    dataSetTypesMap.set(code, {
                        label,
                        value: code,
                        options: dataType === this.openbis.DataType.CONTROLLEDVOCABULARY ? [] : undefined // Use undefined for no options
                    });
                }
            }
        }

        return Array.from(dataSetTypesMap.values());
    }

    createLocatedSXMPreview = async (objId, sxmPermId, sxmFilePath, activeImageIdx, selectedSpectraPreview) => {
        const sxmPreviewConfig = await this.getImagingDatasetPreviewConfig(sxmPermId);
        const spectraConfig = { spectraLocator: true, objId, sxmPreviewConfig, sxmPermId, sxmFilePath, ...selectedSpectraPreview.config }
        selectedSpectraPreview.config = spectraConfig;
        return this.updateImagingDataset(objId, activeImageIdx, selectedSpectraPreview);
    }

    getPathsList = async (datasetList) => {
        const criteria = new this.openbis.DataSetFileSearchCriteria();
        criteria.withDataSet().withCodes().thatIn(datasetList);
        const fetchOptions = new this.openbis.DataSetFileFetchOptions();

        const datasetFiles = await this.openbis.searchFiles(criteria, fetchOptions);

        return datasetFiles.getObjects()
            .filter(file => !file.directory && !file.path.endsWith('.dat'))
            .map(file => [file.dataSetPermId.permId, file.path]);
    };


    getDatasetFilesPath = async (dataset) => {
        const getDatasetCodes = (datasets) => datasets.map(d => d.code);

        if (dataset.sample) {
            const { sample } = dataset;
            if (sample.dataSets?.length) {
                return this.getPathsList(getDatasetCodes(sample.dataSets));
            } else if (sample.parents?.length) {
                return this.getPathsList(sample.parents.flatMap(parent => getDatasetCodes(parent.children.flatMap(child => child.dataSets))));
            } else if (sample.children?.length) {
                return this.getPathsList(sample.children.flatMap(child => getDatasetCodes(child.dataSets)));
            }
        }
        return [];
    };

    loadImagingDataset = async (objId, withProperties = false, withType = false, withDatasetsHierarchy = false) => {
        const fetchOptions = new this.openbis.DataSetFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withType();
        if (withDatasetsHierarchy) {
            //fetchOptions.withSample().withParents().withChildren().withDataSets();
            //fetchOptions.withSample().withChildren().withDataSets();
            fetchOptions.withSample().withDataSets();
        }

        const datasets = await this.openbis.getDataSets(
            [new this.openbis.DataSetPermId(objId)],
            fetchOptions
        )
        const dataset = datasets[objId];

        if (!dataset) return null; // Handle missing dataset

        if (withProperties) return dataset.properties;

        const imagingDataConfig = await this.openbis.fromJson(null, JSON.parse(dataset.properties[constants.IMAGING_DATA_CONFIG]));

        if (withType) {
            const filesPath = withDatasetsHierarchy ? await this.getDatasetFilesPath(dataset) : [];
            return [filesPath, dataset.type.code, imagingDataConfig];
        }

        return imagingDataConfig;
    };

    getImagingDatasetPreviewConfig = async (objId) => {
        const fetchOptions = new this.openbis.DataSetFetchOptions();
        fetchOptions.withProperties();
        const dataset = await this.openbis.getDataSets([new this.openbis.DataSetPermId(objId)], fetchOptions);
        const loadedImgDS = await this.openbis.fromJson(null, JSON.parse(dataset[objId].properties[constants.IMAGING_DATA_CONFIG]));
        return loadedImgDS.images[0]?.previews[0]?.config;
    };

    editImagingDatasetNote = async (permId, note) => {
        const imagingDataset = await this.loadImagingDataset(permId);
        const update = new this.openbis.DataSetUpdate();
        update.setDataSetId(new this.openbis.DataSetPermId(permId));
        update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(imagingDataset));
        update.setProperty(constants.IMAGING_NOTES, note);
        return this.openbis.updateDataSets([update]);
    };

    saveImagingDataset = async (permId, imagingDataset) => {
        const update = new this.openbis.DataSetUpdate();
        update.setDataSetId(new this.openbis.DataSetPermId(permId));
        update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(imagingDataset));
        const totalPreviews = imagingDataset.images.reduce((count, image) => count + image.previews.length, 0);
        update.getMetaData().put(constants.METADATA_PREVIEW_COUNT, totalPreviews.toString());
        return this.openbis.updateDataSets([update]);
    };

    updatePreview = async (permId, imageIdx, preview) => {
        const toUpdateImgDS = await this.loadImagingDataset(permId);
        toUpdateImgDS.images[imageIdx].previews[preview.index] = preview;
        const update = new this.openbis.DataSetUpdate();
        update.setDataSetId(new this.openbis.DataSetPermId(permId));
        update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(toUpdateImgDS));
        return this.openbis.updateDataSets([update]);
    };

    updateImagingDataset = async (objId, activeImageIdx, preview) => {
        const serviceId = new this.openbis.CustomDssServiceCode(constants.IMAGING_CODE);
        const options = new this.openbis.CustomDSSServiceExecutionOptions();
        options.parameters = new ImagingMapper(this.openbis).mapToImagingUpdateParams(objId, activeImageIdx, preview);
        const updatedImagingDataset = await this.openbis.executeCustomDSSService(serviceId, options);
        return this.openbis.fromJson(null, updatedImagingDataset);
    };

    multiExportImagingDataset = async (exportConfig, exportList) => {
        const serviceId = new this.openbis.CustomDssServiceCode(constants.IMAGING_CODE);
        const options = new this.openbis.CustomDSSServiceExecutionOptions();
        options.parameters = new ImagingMapper(this.openbis).mapToImagingMultiExportParams(exportConfig, exportList);
        const exportedImagingDataset = await this.openbis.executeCustomDSSService(serviceId, options);
        return exportedImagingDataset.url;
    };

    exportImagingDataset = async (objId, activeImageIdx, exportConfig, metadata) => {
        const serviceId = new this.openbis.CustomDssServiceCode(constants.IMAGING_CODE);
        const options = new this.openbis.CustomDSSServiceExecutionOptions();
        options.parameters = new ImagingMapper(this.openbis).mapToImagingExportParams(objId, activeImageIdx, exportConfig, metadata);
        const exportedImagingDataset = await this.openbis.executeCustomDSSService(serviceId, options);
        return exportedImagingDataset.url;
    };

    fetchExperimentDataSets = async (objId) => {
        const fetchOptions = new this.openbis.ExperimentFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withDataSets();
        const experiments = await this.openbis.getExperiments([new this.openbis.ExperimentPermId(objId)], fetchOptions);
        return experiments[objId]?.dataSets || []; 
    };

    getRecursiveDescendants = sample => {
        let children = sample.getChildren();
        let datasetList = [];

        children.forEach(child => {
            let childDatasets = this.getRecursiveDescendants(child);
            childDatasets.forEach(dataset => {
                if (!datasetList.some(existing => existing.getCode() === dataset.getCode())) {
                    datasetList.push(dataset);
                }
            });
        });

        sample.getDataSets().forEach(dataset => {
            if (!datasetList.some(existing => existing.getCode() === dataset.getCode())) {
                datasetList.push(dataset);
            }
        });

        return datasetList;
    }

    fetchSampleDataSets = async (objId) => {
        const fetchOptions = new this.openbis.SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();
        fetchOptions.withDataSets();
        fetchOptions.withChildrenUsing(fetchOptions);

        const samples = await this.openbis.getSamples(
            [new this.openbis.SamplePermId(objId)],
            fetchOptions
        );

        const dataSets = this.getRecursiveDescendants(samples[objId]);
        return dataSets;
    }

    fetchDataSetsSortingInfo = (dataSets) => {
        return dataSets.map(dataset => {
            if (constants.METADATA_PREVIEW_COUNT in dataset.metaData) {
                const nDatasets = parseInt(dataset.metaData[constants.METADATA_PREVIEW_COUNT])
                return Array.from(Array(nDatasets), (_, i) => {
                    return { datasetId: dataset.code, sortingId: i, metadata: dataset.metaData }
                });
            }
        }).flat();
    }

    paginateImagingDatasets = async (datasetCodeList, page, pageSize) => {
        let startIdx = page * pageSize;
        const offset = startIdx + pageSize;
        let prevDatasetId = null;
        let loadedImgDS = null;
        let datasetProperties = null;
        let previewContainerList = [];
        while (startIdx < datasetCodeList.length && startIdx < offset) {
            let currDatasetId = datasetCodeList[startIdx].datasetId;
            if (currDatasetId !== prevDatasetId) {
                prevDatasetId = currDatasetId;
                datasetProperties = await this.loadImagingDataset(currDatasetId, true);
                loadedImgDS = await this.openbis.fromJson(null, JSON.parse(datasetProperties[constants.IMAGING_DATA_CONFIG]));
                delete datasetProperties[constants.IMAGING_DATA_CONFIG];
                //console.log(loadedImgDS);
            }
            let partialIdxCount = 0
            for (let imageIdx = 0; imageIdx < loadedImgDS.images.length; imageIdx++) {
                let hypoteticalPreviewIdx = datasetCodeList[startIdx].sortingId;
                for (let previewIdx = hypoteticalPreviewIdx - partialIdxCount;
                    previewIdx < loadedImgDS.images[imageIdx].previews.length && startIdx < offset;
                    previewIdx++, startIdx++) {
                    previewContainerList.push({
                        datasetId: currDatasetId,
                        preview: loadedImgDS.images[imageIdx].previews[previewIdx],
                        imageIdx: imageIdx,
                        select: false,
                        datasetProperties: datasetProperties,
                        exportConfig: loadedImgDS.images[imageIdx].config.exports
                    });
                }
                partialIdxCount += loadedImgDS.images[imageIdx].previews.length
            }
        }
        return previewContainerList
    }

    loadPaginatedGalleryDatasets = async (objId, objType, page, pageSize) => {
        //console.log("loadPaginatedGalleryDatasets: ", objType);
        let dataSets = []
        if (objType === ObjectType.COLLECTION)
            dataSets = await this.fetchExperimentDataSets(objId);
        if (objType === ObjectType.OBJECT)
            dataSets = await this.fetchSampleDataSets(objId);
        const datasetCodeList = this.fetchDataSetsSortingInfo(dataSets);

        const totalCount = datasetCodeList.length;
        const previewContainerList = await this.paginateImagingDatasets(datasetCodeList, page, pageSize);
        //console.log("loadPaginatedGalleryDatasets - previewContainerList: ", previewContainerList);
        return { previewContainerList, totalCount };
    }

    filterAndPaginateImagingDatasets = async (dataSets, page, pageSize, operator, filterText, property) => {
        let startIdx = page * pageSize;
        const offset = startIdx + pageSize;
        let previewContainerListAll = [];

        for (const dataSet of dataSets) {
            let currDatasetId = dataSet.permId.permId;
            let datasetProperties = await this.loadImagingDataset(currDatasetId, true);
            let loadedImgDS = await this.openbis.fromJson(null, JSON.parse(datasetProperties[constants.IMAGING_DATA_CONFIG]));
            delete datasetProperties[constants.IMAGING_DATA_CONFIG];
            //console.log('filterAndPaginateImagingDatasets - loadedImgDS: ', loadedImgDS);

            for (let imageIdx = 0; imageIdx < loadedImgDS.images.length; imageIdx++) {
                for (let previewIdx = 0; previewIdx < loadedImgDS.images[imageIdx].previews.length; previewIdx++) {
                    if (property === constants.IMAGING_TAGS) {
                        const filteringTags = filterText.split(' ');
                        if (operator === messages.get(messages.OPERATOR_OR)) {
                            if (loadedImgDS.images[imageIdx].previews[previewIdx].tags.some(previewTag => filteringTags.includes(previewTag))) {
                                previewContainerListAll.push({
                                    datasetId: currDatasetId,
                                    preview: loadedImgDS.images[imageIdx].previews[previewIdx],
                                    imageIdx: imageIdx,
                                    select: false,
                                    datasetProperties: datasetProperties,
                                    exportConfig: loadedImgDS.images[imageIdx].config.exports
                                });
                            }
                        } else if (operator === messages.get(messages.OPERATOR_AND)) {
                            if (filteringTags.every(filteringTag => loadedImgDS.images[imageIdx].previews[previewIdx].tags.includes(filteringTag))) {
                                previewContainerListAll.push({
                                    datasetId: currDatasetId,
                                    preview: loadedImgDS.images[imageIdx].previews[previewIdx],
                                    imageIdx: imageIdx,
                                    select: false,
                                    datasetProperties: datasetProperties,
                                    exportConfig: loadedImgDS.images[imageIdx].config.exports
                                });
                            }
                        }
                    } else if (property === constants.PREVIEW_COMMENT &&
                        loadedImgDS.images[imageIdx].previews[previewIdx].comment.includes(filterText)) {
                        previewContainerListAll.push({
                            datasetId: currDatasetId,
                            preview: loadedImgDS.images[imageIdx].previews[previewIdx],
                            imageIdx: imageIdx,
                            select: false,
                            datasetProperties: datasetProperties,
                            exportConfig: loadedImgDS.images[imageIdx].config.exports
                        });
                    }
                }
            }
        }

        //console.log('previewContainerListAll: ', previewContainerListAll);
        const previewContainerList = previewContainerListAll.slice(startIdx, offset)
        const totalCount = previewContainerListAll.length
        return { previewContainerList, totalCount }
    }

    filterGallery = async (objId, objType, operator, filterText, property, page, pageSize) => {
        console.log(objId, objType, operator, filterText, property, page, pageSize);

        var dataSets = []
        var totalCount = -1
        var previewContainerList = []
        const criteria = new this.openbis.DataSetSearchCriteria();
        criteria.withAndOperator();
        if (objType === ObjectType.COLLECTION)
            criteria.withExperiment().withPermId().thatEquals(objId);
        else if (objType === ObjectType.OBJECT)
            criteria.withSample().withPermId().thatEquals(objId);

        if ([constants.IMAGING_TAGS, constants.PREVIEW_COMMENT].includes(property)) {

            const fetchOptions = new this.openbis.DataSetFetchOptions();
            fetchOptions.withProperties();

            const searchDataSets = await this.openbis.searchDataSets(
                criteria,
                fetchOptions
            );
            dataSets = searchDataSets.getObjects();
            console.log('filterGallery - fetchDataSets: ', dataSets);

            return await this.filterAndPaginateImagingDatasets(dataSets, page, pageSize, operator, filterText, property);

        } else {
            if (filterText && filterText.trim().length > 0) {
                const subCriteria = criteria.withSubcriteria();
                operator === messages.get(messages.OPERATOR_AND) ? subCriteria.withAndOperator() : subCriteria.withOrOperator();
                const splittedText = filterText.split(' ');
                //console.log('splittedText: ', splittedText);
                for (const value of splittedText) {
                    //console.log('Search on [', property, '] with text [', value, ']')
                    if (property === messages.get(messages.ALL)) {
                        subCriteria.withAnyStringProperty().thatContains(value);
                        //subCriteria.withAnyProperty().thatContains(value);
                    } else {
                        //subCriteria.withStringProperty(property).thatContains(value);
                        subCriteria.withProperty(property).thatContains(value);
                    }
                }
            }
            //console.log('filterGallery - criteria: ', criteria);
            const fetchOptions = new this.openbis.DataSetFetchOptions();
            fetchOptions.withProperties();

            const searchDataSets = await this.openbis.searchDataSets(
                criteria,
                fetchOptions
            );
            dataSets = searchDataSets.getObjects();
            //console.log('filterGallery - searchDataSets: ', dataSets);

            const datasetCodeList = this.fetchDataSetsSortingInfo(dataSets);
            totalCount = datasetCodeList.length;

            previewContainerList = await this.paginateImagingDatasets(datasetCodeList, page, pageSize);
            return { previewContainerList, totalCount };
        }
    }
}
