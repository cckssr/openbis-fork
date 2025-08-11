define([ "stjs", "as/dto/common/update/UpdateObjectsOperationResult" ], function(stjs, UpdateObjectsOperationResult) {
	var UpdateTypeGroupsOperationResult = function(objectIds) {
		UpdateObjectsOperationResult.call(this, objectIds);
	};
	stjs.extend(UpdateTypeGroupsOperationResult, UpdateObjectsOperationResult, [ UpdateObjectsOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.update.UpdateTypeGroupsOperationResult';
		prototype.getMessage = function() {
			return "UpdateTypeGroupsOperationResult";
		};
	}, {});
	return UpdateTypeGroupsOperationResult;
})