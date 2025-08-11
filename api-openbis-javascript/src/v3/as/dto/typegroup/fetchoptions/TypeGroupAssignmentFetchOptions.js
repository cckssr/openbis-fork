define([ "require", "stjs", "as/dto/common/fetchoptions/FetchOptions", "as/dto/person/fetchoptions/PersonFetchOptions",
		"as/dto/typegroup/fetchoptions/TypeGroupFetchOptions", "as/dto/sample/fetchoptions/SampleTypeFetchOptions",
		"as/dto/typegroup/fetchoptions/TypeGroupAssignmentSortOptions" ], function(require, stjs, FetchOptions) {
	var TypeGroupAssignmentFetchOptions = function() {
	};
	stjs.extend(TypeGroupAssignmentFetchOptions, FetchOptions, [ FetchOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions';
		constructor.serialVersionUID = 1;
		prototype.registrator = null;
		prototype.typeGroup = null;
		prototype.sampleType = null;
		prototype.sort = null;

		prototype.withRegistrator = function() {
			if (this.registrator == null) {
				var PersonFetchOptions = require("as/dto/person/fetchoptions/PersonFetchOptions");
				this.registrator = new PersonFetchOptions();
			}
			return this.registrator;
		};
		prototype.withRegistratorUsing = function(fetchOptions) {
			return this.registrator = fetchOptions;
		};
		prototype.hasRegistrator = function() {
			return this.registrator != null;
		};

		prototype.withTypeGroup = function() {
			if (this.typeGroup == null) {
				var TypeGroupFetchOptions = require("as/dto/typegroup/fetchoptions/TypeGroupFetchOptions");
				this.typeGroup = new TypeGroupFetchOptions();
			}
			return this.typeGroup;
		};
		prototype.withTypeGroupUsing = function(fetchOptions) {
			return this.typeGroup = fetchOptions;
		};
		prototype.hasTypeGroup = function() {
			return this.typeGroup != null;
		};

		prototype.withSampleType = function() {
			if (this.sampleType == null) {
				var SampleTypeFetchOptions = require("as/dto/sample/fetchoptions/SampleTypeFetchOptions");
				this.sampleType = new SampleTypeFetchOptions();
			}
			return this.sampleType;
		};
		prototype.withSampleTypeUsing = function(fetchOptions) {
			return this.sampleType = fetchOptions;
		};
		prototype.hasSampleType = function() {
			return this.sampleType != null;
		};

		prototype.sortBy = function() {
			if (this.sort == null) {
				var TypeGroupAssignmentSortOptions = require("as/dto/typegroup/fetchoptions/TypeGroupAssignmentSortOptions");
				this.sort = new TypeGroupAssignmentSortOptions();
			}
			return this.sort;
		};
		prototype.getSortBy = function() {
			return this.sort;
		};
	}, {
		registrator : "PersonFetchOptions",
		typeGroup : "TypeGroupFetchOptions",
		sampleType : "SampleTypeFetchOptions",
		sort : "TypeGroupAssignmentSortOptions"
	});
	return TypeGroupAssignmentFetchOptions;
})