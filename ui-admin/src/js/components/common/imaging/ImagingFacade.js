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

    createLocatedSXMPreview = async (objId, sxmPermId, sxmFilePath, activeImageIdx, selectedDatPreview) => {
        const sxmPreviewConfig = await this.getImagingDatasetPreviewConfig(sxmPermId);
        const spectraConfig = { spectraLocator: true, objId, sxmPreviewConfig, sxmPermId, sxmFilePath, ...selectedDatPreview.config }
        selectedDatPreview.config = spectraConfig;
        const updatedImagingDataset = await this.updateImagingDataset(objId, activeImageIdx, selectedDatPreview);
        return updatedImagingDataset;
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

        if (!dataset) return null;

        if (withProperties) return dataset.properties;

        const imagingDataConfig = await this.openbis.fromJson(null, JSON.parse(dataset.properties[constants.IMAGING_DATA_CONFIG]));

        if (withType) {
            var filesPath = [];
            if (imagingDataConfig.images[0].config.adaptor.includes('NanonisDatAdaptor')) {
                filesPath = withDatasetsHierarchy ? await this.getDatasetFilesPath(dataset) : [];
            }
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
        const startIdx = page * pageSize;
        const endIdx = Math.min(startIdx + pageSize, datasetCodeList.length); // Calculate end index correctly
        const previewContainerList = [];

        let currentDatasetId = null;
        let loadedImgDS = null;
        let datasetProperties = null;

        for (let i = startIdx; i < endIdx; i++) {
            const { datasetId, sortingId } = datasetCodeList[i];

            if (datasetId !== currentDatasetId) {
                currentDatasetId = datasetId;
                datasetProperties = await this.loadImagingDataset(datasetId, true);
                loadedImgDS = await this.openbis.fromJson(null, JSON.parse(datasetProperties[constants.IMAGING_DATA_CONFIG]));
                delete datasetProperties[constants.IMAGING_DATA_CONFIG];
            }

            let previewIndexInDataset = 0;
            for (const image of loadedImgDS.images) {
                for (const preview of image.previews) {
                    if (previewIndexInDataset === sortingId) {
                        previewContainerList.push({
                            datasetId,
                            preview,
                            imageIdx: loadedImgDS.images.indexOf(image), // Get image index
                            select: false,
                            datasetProperties,
                            exportConfig: image.config.exports
                        });
                        break; // Preview found, move to next dataset
                    }
                    previewIndexInDataset++;
                }
                if (previewIndexInDataset > sortingId) break; // Preview found, move to next dataset
            }
        }
        return previewContainerList;
    };

    loadPaginatedGalleryDatasets = async (objId, objType, page, pageSize) => {
        const dataSets = objType === ObjectType.COLLECTION
            ? await this.fetchExperimentDataSets(objId)
            : objType === ObjectType.OBJECT
                ? await this.fetchSampleDataSets(objId)
                : []; // Handle other object types or return empty array

        const datasetCodeList = this.fetchDataSetsSortingInfo(dataSets);
        const totalCount = datasetCodeList.length;
        const previewContainerList = await this.paginateImagingDatasets(datasetCodeList, page, pageSize);

        return { previewContainerList, totalCount };
    };

    filterAndPaginateImagingDatasets = async (dataSets, page, pageSize, operator, filterText, property) => {
        const filteredDatasets = [];

        for (const dataSet of dataSets) {
            const datasetProperties = await this.loadImagingDataset(dataSet.permId.permId, true);
            const loadedImgDS = await this.openbis.fromJson(null, JSON.parse(datasetProperties[constants.IMAGING_DATA_CONFIG]));
            delete datasetProperties[constants.IMAGING_DATA_CONFIG];

            for (const image of loadedImgDS.images) {
                for (const preview of image.previews) {
                    let match = false;
                    if (property === constants.IMAGING_TAGS) {
                        const filteringTags = filterText.split(' ');
                        match = operator === messages.get(messages.OPERATOR_OR)
                            ? preview.tags.some(tag => filteringTags.includes(tag))
                            : operator === messages.get(messages.OPERATOR_AND)
                                ? filteringTags.every(tag => preview.tags.includes(tag))
                                : false; // Handle other operators or invalid input

                    } else if (property === constants.PREVIEW_COMMENT) {
                        match = preview.comment.includes(filterText);
                    }

                    if (match) {
                        filteredDatasets.push({
                            datasetId: dataSet.permId.permId,
                            preview,
                            imageIdx: loadedImgDS.images.indexOf(image),
                            select: false,
                            datasetProperties,
                            exportConfig: image.config.exports
                        });
                    }
                }
            }
        }

        const startIdx = page * pageSize;
        const endIdx = Math.min(startIdx + pageSize, filteredDatasets.length);
        const previewContainerList = filteredDatasets.slice(startIdx, endIdx);
        const totalCount = filteredDatasets.length;

        return { previewContainerList, totalCount };
    };

    filterGallery = async (objId, objType, operator, filterText, property, page, pageSize) => {
        let dataSets = [];

        if (objType === ObjectType.COLLECTION) {
            const criteria = new this.openbis.DataSetSearchCriteria();
            criteria.withExperiment().withPermId().thatEquals(objId);
            const fetchOptions = new this.openbis.DataSetFetchOptions();
            fetchOptions.withProperties();
            const searchDataSets = await this.openbis.searchDataSets(criteria, fetchOptions);
            dataSets = searchDataSets.getObjects();
        } else if (objType === ObjectType.OBJECT) {
            const criteria = new this.openbis.DataSetSearchCriteria();
            criteria.withSample().withPermId().thatEquals(objId);
            const fetchOptions = new this.openbis.DataSetFetchOptions();
            fetchOptions.withProperties();
            const searchDataSets = await this.openbis.searchDataSets(criteria, fetchOptions);
            dataSets = searchDataSets.getObjects();

        }

        if ([constants.IMAGING_TAGS, constants.PREVIEW_COMMENT].includes(property)) {
            return this.filterAndPaginateImagingDatasets(dataSets, page, pageSize, operator, filterText, property);
        } else {
            const criteria = new this.openbis.DataSetSearchCriteria();
            if (objType === ObjectType.COLLECTION) {
                criteria.withExperiment().withPermId().thatEquals(objId);
            } else if (objType === ObjectType.OBJECT) {
                criteria.withSample().withPermId().thatEquals(objId);
            }

            if (filterText && filterText.trim().length > 0) {
                const subCriteria = criteria.withSubcriteria();
                operator === messages.get(messages.OPERATOR_AND) ? subCriteria.withAndOperator() : subCriteria.withOrOperator();
                const splittedText = filterText.split(' ');

                for (const value of splittedText) {
                    if (property === messages.get(messages.ALL)) {
                        subCriteria.withAnyStringProperty().thatContains(value);
                    } else {
                        subCriteria.withProperty(property).thatContains(value);
                    }
                }
            }

            const fetchOptions = new this.openbis.DataSetFetchOptions();
            fetchOptions.withProperties();

            const searchDataSets = await this.openbis.searchDataSets(criteria, fetchOptions);
            dataSets = searchDataSets.getObjects();

            const datasetCodeList = this.fetchDataSetsSortingInfo(dataSets);
            const totalCount = datasetCodeList.length;
            const previewContainerList = await this.paginateImagingDatasets(datasetCodeList, page, pageSize);

            return { previewContainerList, totalCount };
        }
    };
}
