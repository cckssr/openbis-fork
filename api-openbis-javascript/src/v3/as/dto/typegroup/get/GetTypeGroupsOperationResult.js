define([ "stjs", "as/dto/common/get/GetObjectsOperationResult" ], function(stjs, GetObjectsOperationResult) {
	var GetTypeGroupsOperationResult = function(objectMap) {
		GetObjectsOperationResult.call(this, objectMap);
	};
	stjs.extend(GetTypeGroupsOperationResult, GetObjectsOperationResult, [ GetObjectsOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.get.GetTypeGroupsOperationResult';
		prototype.getMessage = function() {
			return "GetTypeGroupsOperationResult";
		};
	}, {});
	return GetTypeGroupsOperationResult;
})