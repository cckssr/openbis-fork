define([ "stjs", "as/dto/common/search/SearchObjectsOperationResult" ], function(stjs, SearchObjectsOperationResult) {
	var SearchTypeGroupsOperationResult = function(searchResult) {
		SearchObjectsOperationResult.call(this, searchResult);
	};
	stjs.extend(SearchTypeGroupsOperationResult, SearchObjectsOperationResult, [ SearchObjectsOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.SearchTypeGroupsOperationResult';
		prototype.getMessage = function() {
			return "SearchTypeGroupsOperationResult";
		};
	}, {});
	return SearchTypeGroupsOperationResult;
})