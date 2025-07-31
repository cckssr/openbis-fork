define([ "stjs", "as/dto/common/create/CreateObjectsOperation" ], function(stjs, CreateObjectsOperation) {
	var CreateTypeGroupAssignmentOperation = function(creations) {
		CreateObjectsOperation.call(this, creations);
	};
	stjs.extend(CreateTypeGroupAssignmentOperation, CreateObjectsOperation, [ CreateObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.CreateTypeGroupAssignmentOperation';
		prototype.getMessage = function() {
			return "CreateTypeGroupAssignmentOperation";
		};
	}, {});
	return CreateTypeGroupAssignmentOperation;
})