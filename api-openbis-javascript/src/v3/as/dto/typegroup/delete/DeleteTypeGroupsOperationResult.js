define([ "stjs", "as/dto/common/delete/DeleteObjectsWithoutTrashOperationResult" ], function(stjs, DeleteObjectsWithoutTrashOperationResult) {
	var DeleteTypeGroupsOperationResult = function() {
		DeleteObjectsWithoutTrashOperationResult.call(this);
	};
	stjs.extend(DeleteTypeGroupsOperationResult, DeleteObjectsWithoutTrashOperationResult, [ DeleteObjectsWithoutTrashOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.delete.DeleteTypeGroupsOperationResult';
		prototype.getMessage = function() {
			return "DeleteTypeGroupsOperationResult";
		};
	}, {});
	return DeleteTypeGroupsOperationResult;
})