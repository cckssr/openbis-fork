/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/delete/DeleteObjectsOperation" ], function(stjs, DeleteObjectsOperation) {
	var DeleteTypeGroupAssignmentOperation = function(objectIds, options) {
		DeleteObjectsOperation.call(this, objectIds, options);
	};
	stjs.extend(DeleteTypeGroupAssignmentOperation, DeleteObjectsOperation, [ DeleteObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.delete.DeleteTypeGroupAssignmentOperation';
		prototype.getMessage = function() {
			return "DeleteTypeGroupAssignmentOperation";
		};
	}, {});
	return DeleteTypeGroupAssignmentOperation;
})