define([ "require", "stjs", "as/dto/common/fetchoptions/SortOptions" ], function(require, stjs, SortOptions) {
	var TypeGroupSortOptions = function() {
		SortOptions.call(this);
	};

	var fields = {
		NAME : "NAME",
	};

	stjs.extend(TypeGroupSortOptions, SortOptions, [ SortOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.fetchoptions.TypeGroupSortOptions';
		constructor.serialVersionUID = 1;
		prototype.name = function() {
			return this.getOrCreateSorting(fields.NAME);
		};
		prototype.getName = function() {
			return this.getSorting(fields.NAME);
		};
	}, {});
	return TypeGroupSortOptions;
})