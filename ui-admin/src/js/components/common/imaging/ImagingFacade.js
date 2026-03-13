import constants from '@src/js/components/common/imaging/constants.js';
import ImagingMapper from "@src/js/components/common/imaging/ImagingMapper";
import messages from "@src/js/common/messages";
import ObjectType from "@src/js/common/consts/objectType";

const SUPPORTED_DATA_TYPE = ["VARCHAR", "MULTILINE_VARCHAR", "CONTROLLEDVOCABULARY"]; //"JSON"

/**
 * Facade for imaging operations.
 * Provides a simplified API for React components.
 *
 * @example
 * const facade = new ImagingFacade(extOpenbis);
 * const datasets = await facade.loadPaginatedGalleryDatasets(objId, objType, 0, 8);
 */
export default class ImagingFacade {

    /**
     * Creates a new ImagingFacade instance.
     *
     * @param {Object} extOpenbis - The openBIS API instance
     */
    constructor(extOpenbis) {
        this.openbis = extOpenbis;
        this.isDataset = true;
    }

    /**
     * Loads imaging vocabulary terms for a given vocabulary code.
     *
     * @param {string} code - Vocabulary code to search for
     * @returns {Promise<Array<Object>>} Array of {label, value} objects
     */
    async loadImagingVocabularyTerms(code) {
        const criteria = new this.openbis.VocabularyTermSearchCriteria();
        criteria.withVocabulary().withCode().thatContains(code);

        const fo = new this.openbis.VocabularyTermFetchOptions();
        fo.sortBy().code().asc();

        const result = await this.openbis.searchVocabularyTerms(criteria, fo);

        return result.getObjects().map(vocabularyTerm => ({ label: vocabularyTerm.label, value: vocabularyTerm.code }));
    }

    /**
     * Loads dataset types and their property assignments for imaging datasets.
     *
     * @returns {Promise<Array<Object>>} Array of property type objects with label, value, and optional options
     */
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

    /**
     * Creates a located SXM preview by combining configs from two datasets.
     *
     * @param {string} objId - Target dataset permanent ID
     * @param {string} sxmPermId - SXM dataset permanent ID
     * @param {string} sxmFilePath - Path to SXM file
     * @param {number} activeImageIdx - Active image index
     * @param {Object} selectedDatPreview - Selected DAT preview object
     * @returns {Promise<Object>} Updated imaging dataset
     */
    createLocatedSXMPreview = async (objId, sxmPermId, sxmFilePath, activeImageIdx, selectedDatPreview) => {
        const sxmPreviewConfig = await this.getImagingDatasetPreviewConfig(sxmPermId);
        const spectraConfig = { spectraLocator: true, objId, sxmPreviewConfig, sxmPermId, sxmFilePath, ...selectedDatPreview.config }
        selectedDatPreview.config = spectraConfig;
        const updatedImagingDataset = await this.updateImagingDataset(objId, activeImageIdx, selectedDatPreview);
        return updatedImagingDataset;
    }

    /**
     * Gets list of file paths for given dataset codes.
     * Filters out directories and .dat files.
     *
     * @param {Array<string>} datasetList - Array of dataset codes
     * @returns {Promise<Array>} Array of [permId, path] tuples
     */
    getPathsList = async (datasetList) => {
        const criteria = new this.openbis.DataSetFileSearchCriteria();
        criteria.withDataSet().withCodes().thatIn(datasetList);
        const fetchOptions = new this.openbis.DataSetFileFetchOptions();

        const datasetFiles = await this.openbis.searchFiles(criteria, fetchOptions);

        return datasetFiles.getObjects()
            .filter(file => !file.directory && !file.path.endsWith('.dat'))
            .map(file => [file.dataSetPermId.permId, file.path]);
    };


    /**
     * Gets file paths for datasets related to a sample.
     *
     * @param {Object} dataset - Dataset object with sample relationship
     * @returns {Promise<Array>} Array of [permId, path] tuples
     */
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

    /**
     * Loads imaging dataset configuration from a dataset's properties.
     *
     * @param {string} objId - Dataset permanent ID
     * @param {boolean} [withProperties=false] - Whether to return raw properties object
     * @param {boolean} [withType=false] - Whether to include dataset type and file paths
     * @param {boolean} [withDatasetsHierarchy=false] - Whether to fetch related datasets for file paths
     * @param {string|null} [objType=null] - Object type (ObjectType.OBJECT for samples, ObjectType.DATA_SET for datasets)
     * @returns {Promise<Object|Array|null>}
     *   - If withProperties: returns dataset properties object
     *   - If withType: returns [filePaths, datasetType, imagingDataConfig]
     *   - Otherwise: returns imagingDataConfig object
     *   - Returns null if dataset not found
     */
    loadImagingDataset = async (objId, withProperties = false, withType = false, withDatasetsHierarchy = false, objType = null) => {
        let skip = false;
        let dataset = null;
        const trySampleFirst = objType === ObjectType.OBJECT || objType === ObjectType.NEW_OBJECT;
        if (this.openbis.hasAfsDataStore() && trySampleFirst) {
            const sampleFetchOptions = new this.openbis.SampleFetchOptions();
            sampleFetchOptions.withProperties();
            sampleFetchOptions.withType();
            if (withDatasetsHierarchy) {
                sampleFetchOptions.withDataSets();
            }
            const samples = await this.openbis.getSamples(
                [new this.openbis.SamplePermId(objId)],
                sampleFetchOptions
            )
            let sample = samples[objId];
            if(sample) {
               skip = true;
               this.isDataset = false;
               dataset = sample;
            }
        }

        if(!skip) {
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
            dataset = datasets[objId];
        }
        if (!dataset) return null;

        if (withProperties) return dataset.properties;

        const imagingDataConfig = await this.openbis.fromJson(null, JSON.parse(dataset.properties[constants.IMAGING_DATA_CONFIG]));

        if (withType) {
            var filesPath = [];
            if (imagingDataConfig.images[0].config.adaptor?.includes('NanonisDatAdaptor')) {
                filesPath = withDatasetsHierarchy ? await this.getDatasetFilesPath(dataset) : [];
            }
            return [filesPath, dataset.type.code, imagingDataConfig];
        }

        return imagingDataConfig;
    };

    /**
     * Gets the preview config from the first preview of the first image in a dataset.
     *
     * @param {string} objId - Dataset permanent ID
     * @returns {Promise<Object|undefined>} Preview config object or undefined if not found
     */
    getImagingDatasetPreviewConfig = async (objId) => {
        const fetchOptions = new this.openbis.DataSetFetchOptions();
        fetchOptions.withProperties();
        const dataset = await this.openbis.getDataSets([new this.openbis.DataSetPermId(objId)], fetchOptions);
        const loadedImgDS = await this.openbis.fromJson(null, JSON.parse(dataset[objId].properties[constants.IMAGING_DATA_CONFIG]));
        return loadedImgDS.images[0]?.previews[0]?.config;
    };

    /**
     * Edits the note property of an imaging dataset.
     *
     * @param {string} permId - Dataset permanent ID
     * @param {string} note - Note text to set
     * @returns {Promise<Object>} Update result
     */
    editImagingDatasetNote = async (permId, note) => {
        const imagingDataset = await this.loadImagingDataset(permId, false, false, false, ObjectType.DATA_SET);
        const update = new this.openbis.DataSetUpdate();
        update.setDataSetId(new this.openbis.DataSetPermId(permId));
        update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(imagingDataset));
        update.setProperty(constants.IMAGING_NOTES, note);
        return this.openbis.updateDataSets([update]);
    };

    /**
         * Saves an imaging dataset configuration.
         * Calculates and sets the preview count metadata.
         *
         * @param {string} permId - Dataset permanent ID
         * @param {Object} imagingDataset - Imaging dataset configuration object
         * @returns {Promise<Object>} Update result
         */
    saveImagingDataset = async (permId, objType, imagingDataset) => {
        if(objType === ObjectType.OBJECT){
            const update = new this.openbis.SampleUpdate();
            update.setSampleId(new this.openbis.SamplePermId(permId))
            update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(imagingDataset));
            const totalPreviews = imagingDataset.images.reduce((count, image) => count + image.previews.length, 0);
            update.getMetaData().put(constants.METADATA_PREVIEW_COUNT, totalPreviews.toString());
            return this.openbis.updateSamples([update]);
        } else {
            const update = new this.openbis.DataSetUpdate();
            update.setDataSetId(new this.openbis.DataSetPermId(permId));
            update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(imagingDataset));
            const totalPreviews = imagingDataset.images.reduce((count, image) => count + image.previews.length, 0);
            update.getMetaData().put(constants.METADATA_PREVIEW_COUNT, totalPreviews.toString());
            return this.openbis.updateDataSets([update]);
        }

    };

    /**
     * Updates a preview within a dataset.
     *
     * @param {string} permId - Dataset permanent ID
     * @param {number} imageIdx - Index of the image
     * @param {Object} preview - Preview object to update
     * @returns {Promise<Object>} Update result
     */
    updatePreview = async (permId, imageIdx, preview) => {
        const toUpdateImgDS = await this.loadImagingDataset(permId, false, false, false, ObjectType.DATA_SET);
        toUpdateImgDS.images[imageIdx].previews[preview.index] = preview;
        const update = new this.openbis.DataSetUpdate();
        update.setDataSetId(new this.openbis.DataSetPermId(permId));
        update.setProperty(constants.IMAGING_DATA_CONFIG, JSON.stringify(toUpdateImgDS));
        return this.openbis.updateDataSets([update]);
    };

    /**
     * Updates an imaging dataset via custom DSS service.
     *
     * @param {string} objId - Dataset permanent ID
     * @param {number} activeImageIdx - Active image index
     * @param {Object} preview - Preview object to update
     * @returns {Promise<Object>} Updated imaging dataset
     */
    updateImagingDataset = async (objId, activeImageIdx, preview) => {
        const serviceId = new this.openbis.CustomASServiceCode(constants.IMAGING_CODE);
        const options = new this.openbis.CustomASServiceExecutionOptions();
        options.parameters = new ImagingMapper(this.openbis).mapToImagingUpdateParams(objId, activeImageIdx, preview);
        const updatedImagingDataset = await this.openbis.executeService(serviceId, options);
        return this.openbis.fromJson(null, updatedImagingDataset);
    };

    /**
     * Exports multiple imaging dataset previews in a single operation.
     *
     * @param {Object} exportConfig - Export configuration object
     * @param {Array<Object>} exportList - Array of preview objects to export
     * @returns {Promise<Object>} URL to download the exported archive
     */
    multiExportImagingDataset = async (exportConfig, exportList) => {
        const serviceId = new this.openbis.CustomASServiceCode(constants.IMAGING_CODE);
        const options = new this.openbis.CustomASServiceExecutionOptions();
        options.parameters = new ImagingMapper(this.openbis).mapToImagingMultiExportParams(exportConfig, exportList);
        return await this.openbis.executeService(serviceId, options);
    };

    /**
     * Exports a single imaging dataset preview.
     *
     * @param {string} objId - Dataset permanent ID
     * @param {number} activeImageIdx - Index of the active image
     * @param {Object} exportConfig - Export configuration object
     * @param {Object} metadata - Metadata to include in export
     * @returns {Promise<Object>} URL to download the exported file
     */
    exportImagingDataset = async (objId, activeImageIdx, exportConfig, metadata) => {
        const serviceId = new this.openbis.CustomASServiceCode(constants.IMAGING_CODE);
        const options = new this.openbis.CustomASServiceExecutionOptions();
        options.parameters = new ImagingMapper(this.openbis).mapToImagingExportParams(objId, activeImageIdx, exportConfig, metadata);
        return await this.openbis.executeService(serviceId, options);
    };

    /**
     * Fetches datasets for an experiment.
     *
     * @param {string} objId - Experiment permanent ID
     * @returns {Promise<Array>} Array of dataset objects filtered to imaging types
     */
    fetchExperimentDataSets = async (objId) => {
        const fetchOptions = new this.openbis.ExperimentFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withDataSets();
        fetchOptions.withDataSets().withType();
        const experiments = await this.openbis.getExperiments([new this.openbis.ExperimentPermId(objId)], fetchOptions);
        const expDatasets = experiments[objId]?.dataSets?.filter(dataset => dataset.type.code === constants.IMAGING_DATA || dataset.type.code === constants.USER_DEFINED_IMAGING_DATA) || [];
        return expDatasets;
    };

    /**
     * Recursively gets all datasets from a sample and its children.
     *
     * @param {Object} sample - Sample object
     * @returns {Array} Array of dataset objects
     */
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

    /**
     * Fetches datasets for a sample, including recursive descendants.
     *
     * @param {string} objId - Sample permanent ID
     * @returns {Promise<Array>} Array of dataset objects from sample and all descendants, filtered to imaging types
     */
    fetchSampleDataSets = async (objId) => {
        const fetchOptions = new this.openbis.SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();
        fetchOptions.withDataSets();
        fetchOptions.withDataSets().withType();
        fetchOptions.withChildrenUsing(fetchOptions);

        const samples = await this.openbis.getSamples(
            [new this.openbis.SamplePermId(objId)],
            fetchOptions
        );

        const dataSets = this.getRecursiveDescendants(samples[objId]);
        const sampleDatasets = dataSets.filter(dataset => dataset.type.code === constants.IMAGING_DATA || dataset.type.code === constants.USER_DEFINED_IMAGING_DATA) || [];
        return sampleDatasets;
    }

    /**
     * Calculates preview sorting information for datasets without loading the dataset properties.
     * Creates an array of preview entries with dataset ID and sorting index.
     *
     * @param {Array<Object>} dataSets - Array of dataset objects with metadata
     * @returns {Array<Object>} Array of {datasetId, sortingId, metadata} objects
     */
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

    /**
     * Paginates imaging datasets by loading preview containers for a specific page.
     *
     * This method efficiently loads only the datasets needed for the current page,
     * caching dataset properties to avoid redundant API calls.
     *
     * @param {Array<Object>} datasetCodeList - Array of {datasetId, sortingId} objects
     * @param {number} page - Current page (0-indexed)
     * @param {number} pageSize - Number of items per page
     * @returns {Promise<Array<Object>>} Array of preview container objects
     */
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
                datasetProperties = await this.loadImagingDataset(datasetId, true, false, false, ObjectType.DATA_SET);
                loadedImgDS = await this.openbis.fromJson(null, JSON.parse(datasetProperties[constants.IMAGING_DATA_CONFIG]));
                delete datasetProperties[constants.IMAGING_DATA_CONFIG];
            }

            let previewIndexInDataset = 0;
            for (const image of loadedImgDS.images) {
                for (const [index, preview] of image.previews.entries()) {
                    if (previewIndexInDataset === sortingId) {
                        previewContainerList.push({
                            datasetId,
                            preview,
                            previewIdx: index,
                            imageIdx: loadedImgDS.images.indexOf(image), // Get image index
                            imageMetadata: image.metadata,
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

    /**
     * Loads paginated gallery datasets for an experiment or sample.
     *
     * @param {string} objId - Object ID (experiment or sample permanent ID)
     * @param {string} objType - Object type (ObjectType.COLLECTION or ObjectType.OBJECT)
     * @param {number} page - Current page (0-indexed)
     * @param {number} pageSize - Number of items per page
     * @returns {Promise<Object>} Object with previewContainerList and totalCount
     */
    loadPaginatedGalleryDatasets = async (objId, objType, page, pageSize) => {
        const dataSets = objType === ObjectType.COLLECTION
            ? await this.fetchExperimentDataSets(objId)
            : objType === ObjectType.OBJECT
                ? await this.fetchSampleDataSets(objId)
                : []; // Handle other object types or return empty array

        const datasetCodeList = this.fetchDataSetsSortingInfo(dataSets);
        //console.log('loadPaginatedGalleryDatasets - datasetCodeList: ', datasetCodeList);
        const totalCount = datasetCodeList.length;
        const previewContainerList = await this.paginateImagingDatasets(datasetCodeList, page, pageSize);

        return { previewContainerList, totalCount };
    };

    /**
     * Filters and paginates previews based on preview properties (tags, comments).
     *
     * @param {Array<Object>} dataSets - Array of dataset objects
     * @param {number} page - Current page (0-indexed)
     * @param {number} pageSize - Number of items per page
     * @param {string} operator - Filter operator ('AND' or 'OR')
     * @param {string} filterText - Text to filter by
     * @param {string} property - Property to filter on (IMAGING_TAGS or PREVIEW_COMMENT)
     * @returns {Promise<Object>} Object with previewContainerList and totalCount
     */
    filterAndPaginateImagingDatasets = async (dataSets, page, pageSize, operator, filterText, property) => {
        const filteredDatasets = [];

        for (const dataSet of dataSets) {
            const datasetProperties = await this.loadImagingDataset(dataSet.permId.permId, true, false, false, ObjectType.DATA_SET);
            const loadedImgDS = await this.openbis.fromJson(null, JSON.parse(datasetProperties[constants.IMAGING_DATA_CONFIG]));
            delete datasetProperties[constants.IMAGING_DATA_CONFIG];

            let previewIndexInDataset = 0;
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
                        // We treat new lines as spaces.
                        const singleLineComment = preview.comment.replace(/(\r\n|\n|\r)/g, ' ');
                        match = singleLineComment.includes(filterText);
                    }

                    if (match) {
                        filteredDatasets.push({
                            datasetId: dataSet.permId.permId,
                            preview,
                            previewIdx: previewIndexInDataset,
                            imageIdx: loadedImgDS.images.indexOf(image),
                            select: false,
                            datasetProperties,
                            exportConfig: image.config.exports
                        });
                    }
                    previewIndexInDataset++;
                }
            }
        }

        const startIdx = page * pageSize;
        const endIdx = Math.min(startIdx + pageSize, filteredDatasets.length);
        const previewContainerList = filteredDatasets.slice(startIdx, endIdx);
        const totalCount = filteredDatasets.length;

        return { previewContainerList, totalCount };
    };

    /**
     * Filters gallery datasets based on various criteria.
     *
     * @param {string} objId - Object ID (experiment or sample)
     * @param {string} objType - Object type (ObjectType.COLLECTION or ObjectType.OBJECT)
     * @param {string} operator - Filter operator ('AND' or 'OR')
     * @param {string} filterText - Text to filter by
     * @param {string} property - Property to filter on
     * @param {number} page - Current page (0-indexed)
     * @param {number} pageSize - Number of items per page
     * @returns {Promise<Object>} Object with previewContainerList and totalCount
     */
    filterGallery = async (objId, objType, operator, filterText, property, page, pageSize) => {
        let dataSets = [];

        // TODO: here everything is loaded and then filtered
        // Consider adding fetchOptions.from() and fetchOptions.count()
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
