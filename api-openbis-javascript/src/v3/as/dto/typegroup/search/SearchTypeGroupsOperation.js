define([ "stjs", "as/dto/common/search/SearchObjectsOperation" ], function(stjs, SearchObjectsOperation) {
	var SearchTypeGroupsOperation = function(criteria, fetchOptions) {
		SearchObjectsOperation.call(this, criteria, fetchOptions);
	};
	stjs.extend(SearchTypeGroupsOperation, SearchObjectsOperation, [ SearchObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.SearchTypeGroupsOperation';
		prototype.getMessage = function() {
			return "SearchTypeGroupsOperation";
		};
	}, {});
	return SearchTypeGroupsOperation;
})