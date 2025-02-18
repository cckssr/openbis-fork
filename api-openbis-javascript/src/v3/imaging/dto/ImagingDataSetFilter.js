define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var ImagingDataSetFilter = function() {
	};
	stjs.extend(ImagingDataSetFilter, null, [], function(constructor, prototype) {
		prototype['@type'] = 'imaging.dto.ImagingDataSetFilter';
		constructor.serialVersionUID = 1;
		prototype.name = null;
		prototype.parameters = null;

		prototype.getName = function() {
			return this.name;
		};
		prototype.setName = function(name) {
			this.name = name;
		};
		prototype.getParameters = function() {
			return this.parameters;
		};
		prototype.setParameters = function(parameters) {
			this.parameters = parameters;
		};

		prototype.toString = function() {
            return "ImagingDataSetFilter: " + this.name;
        };

	}, {
		parameters : {
            name : "Map",
            arguments : [ "String", "Serializable" ]
        }
	});
	return ImagingDataSetFilter;
})