define([ "stjs", "as/dto/common/search/SearchObjectsOperation" ], function(stjs, SearchObjectsOperation) {
	var SearchTypeGroupAssignmentsOperation = function(criteria, fetchOptions) {
		SearchObjectsOperation.call(this, criteria, fetchOptions);
	};
	stjs.extend(SearchTypeGroupAssignmentsOperation, SearchObjectsOperation, [ SearchObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.SearchTypeGroupAssignmentsOperation';
		prototype.getMessage = function() {
			return "SearchTypeGroupAssignmentsOperation";
		};
	}, {});
	return SearchTypeGroupAssignmentsOperation;
})