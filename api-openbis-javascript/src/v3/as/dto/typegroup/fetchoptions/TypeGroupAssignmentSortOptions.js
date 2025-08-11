define([ "require", "stjs", "as/dto/common/fetchoptions/SortOptions" ], function(require, stjs, SortOptions) {
	var TypeGroupAssignmentSortOptions = function() {
		SortOptions.call(this);
	};

	stjs.extend(TypeGroupAssignmentSortOptions, SortOptions, [ SortOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.fetchoptions.TypeGroupAssignmentSortOptions';
		constructor.serialVersionUID = 1;
	}, {});
	return TypeGroupAssignmentSortOptions;
})