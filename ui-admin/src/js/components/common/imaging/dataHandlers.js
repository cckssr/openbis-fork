import messages from "@src/js/common/messages.js";
import constants from '@src/js/components/common/imaging/constants.js';

export async function loadGalleryViewFilters(imagingFacade, setDataSetTypes) {
	const imagingDatasetTypes = await imagingFacade.loadDataSetTypes();
	for (const dataType of imagingDatasetTypes) {
		if(dataType.options){
			dataType.options = await imagingFacade.loadImagingVocabularyTerms(dataType.value);
		}
	}
	//console.log('loadGalleryViewFilters - imagingDatasetTypes', imagingDatasetTypes);
	imagingDatasetTypes.push({label: messages.get(messages.ALL_PROPERTIES), value: messages.ALL});
	imagingDatasetTypes.push({label: '(Preview) Comment', value: 'PREVIEW_COMMENT'});
	setDataSetTypes(imagingDatasetTypes);
}

export async function loadPreviewsInfo(imagingFacade, objId, objType, galleryFilter, paging, setPreviewsInfo, setIsLoaded) {
	let {previewContainerList, totalCount} = galleryFilter.text.length >= 3 ?
                await imagingFacade.filterGallery(objId, objType, galleryFilter.operator, galleryFilter.text, galleryFilter.property, paging.page, paging.pageSize)
                : await imagingFacade.loadPaginatedGalleryDatasets(objId, objType, paging.page, paging.pageSize)
            setPreviewsInfo({previewContainerList, totalCount});
            setIsLoaded(true);
}
