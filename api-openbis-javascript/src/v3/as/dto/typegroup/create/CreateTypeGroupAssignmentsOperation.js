define([ "stjs", "as/dto/common/create/CreateObjectsOperation" ], function(stjs, CreateObjectsOperation) {
	var CreateTypeGroupAssignmentsOperation = function(creations) {
		CreateObjectsOperation.call(this, creations);
	};
	stjs.extend(CreateTypeGroupAssignmentsOperation, CreateObjectsOperation, [ CreateObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.CreateTypeGroupAssignmentsOperation';
		prototype.getMessage = function() {
			return "CreateTypeGroupAssignmentsOperation";
		};
	}, {});
	return CreateTypeGroupAssignmentsOperation;
})