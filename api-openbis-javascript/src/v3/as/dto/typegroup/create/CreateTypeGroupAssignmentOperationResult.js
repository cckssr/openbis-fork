define([ "stjs", "as/dto/common/create/CreateObjectsOperationResult"], function(stjs, CreateObjectsOperationResult) {
	var CreateTypeGroupAssignmentOperationResult = function(objectIds) {
        CreateObjectsOperationResult.call(this, objectIds);
    };
	stjs.extend(CreateTypeGroupAssignmentOperationResult, CreateObjectsOperationResult, [CreateObjectsOperationResult], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.CreateTypeGroupAssignmentOperationResult';
        prototype.getMessage = function() {
			return "CreateTypeGroupAssignmentOperationResult";
		};
	}, {});
	return CreateTypeGroupAssignmentOperationResult;
})