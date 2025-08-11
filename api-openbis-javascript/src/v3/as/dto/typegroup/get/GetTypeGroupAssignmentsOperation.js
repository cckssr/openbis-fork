define([ "stjs", "as/dto/common/get/GetObjectsOperation" ], function(stjs, GetObjectsOperation) {
	var GetTypeGroupAssignmentsOperation = function(objectIds, fetchOptions) {
		GetObjectsOperation.call(this, objectIds, fetchOptions);
	};
	stjs.extend(GetTypeGroupAssignmentsOperation, GetObjectsOperation, [ GetObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.get.GetTypeGroupAssignmentsOperation';
		prototype.getMessage = function() {
			return "GetTypeGroupAssignmentsOperation";
		};
	}, {});
	return GetTypeGroupAssignmentsOperation;
})