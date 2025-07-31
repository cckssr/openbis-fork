define([ "stjs", "as/dto/common/create/CreateObjectsOperation" ], function(stjs, CreateObjectsOperation) {
	var CreateTypeGroupsOperation = function(creations) {
		CreateObjectsOperation.call(this, creations);
	};
	stjs.extend(CreateTypeGroupsOperation, CreateObjectsOperation, [ CreateObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.CreateTypeGroupsOperation';
		prototype.getMessage = function() {
			return "CreateTypeGroupsOperation";
		};
	}, {});
	return CreateTypeGroupsOperation;
})