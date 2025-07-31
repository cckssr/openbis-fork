/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/delete/DeleteObjectsOperation" ], function(stjs, DeleteObjectsOperation) {
	var DeleteTypeGroupsOperation = function(objectIds, options) {
		DeleteObjectsOperation.call(this, objectIds, options);
	};
	stjs.extend(DeleteTypeGroupsOperation, DeleteObjectsOperation, [ DeleteObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.delete.DeleteTypeGroupsOperation';
		prototype.getMessage = function() {
			return "DeleteTypeGroupsOperation";
		};
	}, {});
	return DeleteTypeGroupsOperation;
})