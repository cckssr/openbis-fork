import constants from "@src/js/components/common/imaging/constants.js";

export default class ImagingMapper{

    constructor(extOpenbis) {
        this.openbis = extOpenbis;
    }

    getImagingDataSetPreview(config, format, bytes, width, height, index, show, metadata, tags, comment, filterConfig) {
        let imagingDataSetPreview = new this.openbis.ImagingDataSetPreview();
        imagingDataSetPreview.config = config;
        imagingDataSetPreview.format = format;
        imagingDataSetPreview.bytes = bytes;
        imagingDataSetPreview.width = width;
        imagingDataSetPreview.height = height;
        imagingDataSetPreview.index = index;
        imagingDataSetPreview.show = show;
        imagingDataSetPreview.metadata = metadata;
        imagingDataSetPreview.tags = tags;
        imagingDataSetPreview.comment = comment;
        imagingDataSetPreview.filterConfig = filterConfig;
        return imagingDataSetPreview;
    }

    mapToImagingDataSetPreview(preview) {
        let imagingDataSetPreview = new this.openbis.ImagingDataSetPreview();
        imagingDataSetPreview.config = preview.config;
        imagingDataSetPreview.format = preview.format;
        imagingDataSetPreview.bytes = preview.bytes;
        imagingDataSetPreview.width = preview.width;
        imagingDataSetPreview.height = preview.height;
        imagingDataSetPreview.index = preview.index;
        imagingDataSetPreview.show = preview.show;
        imagingDataSetPreview.metadata = preview.metadata;
        imagingDataSetPreview.tags = preview.tags;
        imagingDataSetPreview.comment = preview.comment;
        imagingDataSetPreview.filterConfig = preview.filterConfig;
        return imagingDataSetPreview;
    }

    mapToImagingUpdateParams(objId, activeImageIdx, preview) {
        return {
            "type" : "preview",
            "permId" : objId,
            "error" : null,
            "index": activeImageIdx,
            "preview" :  this.mapToImagingDataSetPreview(preview)
        };
    }

    mapToImagingDataSetExportConfig(exportConfig) {
        let imagingDataSetExportConfig = new this.openbis.ImagingDataSetExportConfig();
        imagingDataSetExportConfig.imageFormat = exportConfig['image-format'];
        imagingDataSetExportConfig.archiveFormat = exportConfig['archive-format'];
        imagingDataSetExportConfig.resolution = exportConfig['resolution'];
        imagingDataSetExportConfig.include = exportConfig['include'].map(c => c.toUpperCase().replace(' ', '_'));
        return imagingDataSetExportConfig;
    }

    mapToImagingExportParams(objId, activeImageIdx, exportConfig, metadata) {
        let imagingDataSetExportConfig = this.mapToImagingDataSetExportConfig(exportConfig);
        let imagingDataSetExport = new this.openbis.ImagingDataSetExport();
        imagingDataSetExport.config = imagingDataSetExportConfig;
        imagingDataSetExport.metadata = metadata;
        return {
            "type" : constants.EXPORT_TYPE,
            "permId" : objId,
            "error" : null,
            "index" : activeImageIdx,
            "url" : null,
            "export" :  imagingDataSetExport
        };
    }

    mapToImagingMultiExportParams(exportConfig, exportList) {
        let imagingDataSetExportConfig = this.mapToImagingDataSetExportConfig(exportConfig);
        const imagingDataSetMultiExportList = exportList.map(previewObj => {
            let imagingDataSetMultiExport = new this.openbis.ImagingDataSetMultiExport();
            imagingDataSetMultiExport.config = imagingDataSetExportConfig;
            imagingDataSetMultiExport.metadata = previewObj.metadata;
            imagingDataSetMultiExport.permId = previewObj.datasetId;
            imagingDataSetMultiExport.imageIndex = previewObj.imageIdx;
            imagingDataSetMultiExport.previewIndex = previewObj.preview.index;
            return imagingDataSetMultiExport;
        });
        return {
            "type" : constants.MULTI_EXPORT_TYPE,
            "error" : null,
            "url" : null,
            "exports" :  imagingDataSetMultiExportList
        };
    }

}