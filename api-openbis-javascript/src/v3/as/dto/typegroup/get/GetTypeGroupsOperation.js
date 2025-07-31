define([ "stjs", "as/dto/common/get/GetObjectsOperation" ], function(stjs, GetObjectsOperation) {
	var GetTypeGroupsOperation = function(objectIds, fetchOptions) {
		GetObjectsOperation.call(this, objectIds, fetchOptions);
	};
	stjs.extend(GetTypeGroupsOperation, GetObjectsOperation, [ GetObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.get.GetTypeGroupsOperation';
		prototype.getMessage = function() {
			return "GetTypeGroupsOperation";
		};
	}, {});
	return GetTypeGroupsOperation;
})