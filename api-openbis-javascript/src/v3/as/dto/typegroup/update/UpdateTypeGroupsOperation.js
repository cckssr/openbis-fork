define([ "stjs", "as/dto/common/update/UpdateObjectsOperation" ], function(stjs, UpdateObjectsOperation) {
	var UpdateTypeGroupsOperation = function(updates) {
		UpdateObjectsOperation.call(this, updates);
	};
	stjs.extend(UpdateTypeGroupsOperation, UpdateObjectsOperation, [ UpdateObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.update.UpdateTypeGroupsOperation';
		prototype.getMessage = function() {
			return "UpdateTypeGroupsOperation";
		};
	}, {});
	return UpdateTypeGroupsOperation;
})