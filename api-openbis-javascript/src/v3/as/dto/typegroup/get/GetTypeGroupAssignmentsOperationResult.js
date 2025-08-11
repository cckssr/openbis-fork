define([ "stjs", "as/dto/common/get/GetObjectsOperationResult" ], function(stjs, GetObjectsOperationResult) {
	var GetTypeGroupAssignmentsOperationResult = function(objectMap) {
		GetObjectsOperationResult.call(this, objectMap);
	};
	stjs.extend(GetTypeGroupAssignmentsOperationResult, GetObjectsOperationResult, [ GetObjectsOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.get.GetTypeGroupAssignmentsOperationResult';
		prototype.getMessage = function() {
			return "GetTypeGroupAssignmentsOperationResult";
		};
	}, {});
	return GetTypeGroupAssignmentsOperationResult;
})