define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var ImagingDataSetPropertyConfig = function() {
	};
	stjs.extend(ImagingDataSetPropertyConfig, null, [], function(constructor, prototype) {
		prototype['@type'] = 'imaging.dto.ImagingDataSetPropertyConfig';
		constructor.serialVersionUID = 1;
		prototype.images = null;
		prototype.metadata = null;

		prototype.getImages = function() {
            return this.images;
        };
        prototype.setImages = function(images) {
            this.images = images;
        };
        prototype.getMetadata = function() {
            return this.metadata;
        };
        prototype.setMetadata = function(metadata) {
            this.metadata = metadata;
        };

		prototype.toString = function() {
            return "ImagingDataSetPropertyConfig: " + this.label;
        };

	}, {
		images : {
            name : "List",
            arguments : [ "ImagingDataSetImage"]
        },
        metadata : {
            name : "Map",
            arguments : [ "String", "Serializable" ]
        }
	});
	return ImagingDataSetPropertyConfig;
})