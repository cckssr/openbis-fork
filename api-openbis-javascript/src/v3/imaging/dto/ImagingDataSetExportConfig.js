define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var ImagingDataSetExportConfig = function() {
	};
	stjs.extend(ImagingDataSetExportConfig, null, [], function(constructor, prototype) {
		prototype['@type'] = 'imaging.dto.ImagingDataSetExportConfig';
		constructor.serialVersionUID = 1;
		prototype.archiveFormat = null;
		prototype.imageFormat = null;
		prototype.resolution = null;
		prototype.include = null;
		prototype.customOptions = null;

		prototype.getArchiveFormat = function() {
            return this.archiveFormat;
        };
        prototype.setArchiveFormat = function(archiveFormat) {
            this.archiveFormat = archiveFormat;
        };
        prototype.getImageFormat = function() {
            return this.imageFormat;
        };
        prototype.setImageFormat = function(imageFormat) {
            this.imageFormat = imageFormat;
        };
		prototype.getResolution = function() {
			return this.resolution;
		};
		prototype.setResolution = function(resolution) {
			this.resolution = resolution;
		};
		prototype.getResolution = function() {
            return this.resolution;
        };
        prototype.setResolution = function(resolution) {
            this.resolution = resolution;
        };
        prototype.getInclude = function() {
            return this.include;
        };
        prototype.setInclude = function(include) {
            this.include = include;
        };
        prototype.getCustomOptions = function() {
            return this.customOptions;
        };
        prototype.setCustomOptions = function(customOptions) {
            this.customOptions = customOptions;
        };
		prototype.toString = function() {
            return "ImagingDataSetExportConfig: " + this.config;
        };

	}, {
		exports : {
            name : "List",
            arguments : [ "ImagingExportIncludeOptions"]
        },
        customOptions : {
            name : "Map",
            arguments : [ "String", "String" ]
        }
	});
	return ImagingDataSetExportConfig;
})