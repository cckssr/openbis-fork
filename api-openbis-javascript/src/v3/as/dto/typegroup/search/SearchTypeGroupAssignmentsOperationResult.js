define([ "stjs", "as/dto/common/search/SearchObjectsOperationResult" ], function(stjs, SearchObjectsOperationResult) {
	var SearchTypeGroupAssignmentsOperationResult = function(searchResult) {
		SearchObjectsOperationResult.call(this, searchResult);
	};
	stjs.extend(SearchTypeGroupAssignmentsOperationResult, SearchObjectsOperationResult, [ SearchObjectsOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.SearchTypeGroupAssignmentsOperationResult';
		prototype.getMessage = function() {
			return "SearchTypeGroupAssignmentsOperationResult";
		};
	}, {});
	return SearchTypeGroupAssignmentsOperationResult;
})