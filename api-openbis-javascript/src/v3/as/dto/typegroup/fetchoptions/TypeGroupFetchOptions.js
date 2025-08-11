define([ "require", "stjs", "as/dto/common/fetchoptions/FetchOptions", "as/dto/person/fetchoptions/PersonFetchOptions",
		"as/dto/typegroup/fetchoptions/TypeGroupAssignmentFetchOptions",
		"as/dto/typegroup/fetchoptions/TypeGroupSortOptions" ], function(require, stjs, FetchOptions) {
	var TypeGroupFetchOptions = function() {
	};
	stjs.extend(TypeGroupFetchOptions, FetchOptions, [ FetchOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.fetchoptions.TypeGroupFetchOptions';
		constructor.serialVersionUID = 1;
		prototype.registrator = null;
		prototype.modifier = null;
		prototype.typeGroupAssignments = null;
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

		prototype.withTypeGroupAssignments = function() {
			if (this.typeGroupAssignments == null) {
				var TypeGroupAssignmentFetchOptions = require("as/dto/typegroup/fetchoptions/TypeGroupAssignmentFetchOptions");
				this.typeGroupAssignments = new TypeGroupAssignmentFetchOptions();
			}
			return this.typeGroupAssignments;
		};
		prototype.withTypeGroupAssignmentsUsing = function(fetchOptions) {
			return this.typeGroupAssignments = fetchOptions;
		};
		prototype.hasTypeGroupAssignments = function() {
			return this.typeGroupAssignments != null;
		};

		prototype.withModifier = function() {
			if (this.modifier == null) {
				var PersonFetchOptions = require("as/dto/person/fetchoptions/PersonFetchOptions");
				this.modifier = new PersonFetchOptions();
			}
			return this.modifier;
		};
		prototype.withModifierUsing = function(fetchOptions) {
			return this.modifier = fetchOptions;
		};
		prototype.hasModifier = function() {
			return this.modifier != null;
		};

		prototype.sortBy = function() {
			if (this.sort == null) {
				var TypeGroupSortOptions = require("as/dto/typegroup/fetchoptions/TypeGroupSortOptions");
				this.sort = new TypeGroupSortOptions();
			}
			return this.sort;
		};
		prototype.getSortBy = function() {
			return this.sort;
		};
	}, {
		registrator : "PersonFetchOptions",
		modifier : "PersonFetchOptions",
		typeGroupAssignments : "TypeGroupAssignmentFetchOptions",
		sort : "TypeGroupSortOptions"
	});
	return TypeGroupFetchOptions;
})