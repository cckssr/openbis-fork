import messages from "@src/js/common/messages.js";
import constants from '@src/js/components/common/imaging/constants.js';

export async function loadGalleryViewFilters(imagingFacade, setDataSetTypes) {
	const imagingDatasetTypes = await imagingFacade.loadDataSetTypes();
	//imagingDatasetTypes.push({label: messages.get(messages.ALL_PROPERTIES), value: messages.ALL});
	imagingDatasetTypes.push({ label: '(Preview) Tags', value: constants.IMAGING_TAGS, options: [] });
	imagingDatasetTypes.push({ label: '(Preview) Comment', value: constants.PREVIEW_COMMENT });
	for (const dataType of imagingDatasetTypes) {
		if (dataType.options) {
			dataType.options = await imagingFacade.loadImagingVocabularyTerms(dataType.value);
		}
	}
	//console.log('loadGalleryViewFilters - imagingDatasetTypes', imagingDatasetTypes);
	setDataSetTypes(imagingDatasetTypes);
}

export async function loadPreviewsInfo(imagingFacade, objId, objType, galleryFilter, paging) {
	if (galleryFilter.text.length >= 3 ||
		(galleryFilter.text.length >= 1 && [constants.DEFAULT_DATASET_VIEW, constants.IMAGING_TAGS].includes(galleryFilter.property))) {
		return await imagingFacade.filterGallery(objId, objType, galleryFilter.operator, galleryFilter.text, galleryFilter.property, paging.page, paging.pageSize)
	} else {
		return await imagingFacade.loadPaginatedGalleryDatasets(objId, objType, paging.page, paging.pageSize)
	}
}

export async function loadImagingVocabularyTerms(imagingFacade, setImagingTags) {
	let imagingTags = await imagingFacade.loadImagingVocabularyTerms(constants.IMAGING_TAGS);
	setImagingTags(imagingTags);
}
