define([ "stjs", "as/dto/common/delete/DeleteObjectsWithoutTrashOperationResult" ], function(stjs, DeleteObjectsWithoutTrashOperationResult) {
	var DeleteTypeGroupAssignmentOperationResult = function() {
		DeleteObjectsWithoutTrashOperationResult.call(this);
	};
	stjs.extend(DeleteTypeGroupAssignmentOperationResult, DeleteObjectsWithoutTrashOperationResult, [ DeleteObjectsWithoutTrashOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.delete.DeleteTypeGroupAssignmentOperationResult';
		prototype.getMessage = function() {
			return "DeleteTypeGroupAssignmentOperationResult";
		};
	}, {});
	return DeleteTypeGroupAssignmentOperationResult;
})