define([ "require", "stjs", "as/dto/common/fetchoptions/EntityWithPropertiesSortOptions" ], function(require, stjs, EntityWithPropertiesSortOptions) {
	var SampleSortOptions = function() {
		EntityWithPropertiesSortOptions.call(this);
	};

	var fields = {
		IDENTIFIER : "IDENTIFIER",
		IMMUTABLE_DATA_DATE : "IMMUTABLE_DATA_DATE"
	};

	stjs.extend(SampleSortOptions, EntityWithPropertiesSortOptions, [ EntityWithPropertiesSortOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.sample.fetchoptions.SampleSortOptions';
		constructor.serialVersionUID = 1;

		prototype.identifier = function() {
			return this.getOrCreateSorting(fields.IDENTIFIER);
		};
		prototype.getIdentifier = function() {
			return this.getSorting(fields.IDENTIFIER);
		};
		prototype.immutableDataDate = function() {
			return this.getOrCreateSorting(fields.IMMUTABLE_DATA_DATE);
		};
		prototype.getImmutableDataDate = function() {
			return this.getSorting(fields.IMMUTABLE_DATA_DATE);
		};
	}, {});
	return SampleSortOptions;
})