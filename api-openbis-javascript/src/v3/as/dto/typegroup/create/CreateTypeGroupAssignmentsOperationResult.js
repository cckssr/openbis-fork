define([ "stjs", "as/dto/common/create/CreateObjectsOperationResult"], function(stjs, CreateObjectsOperationResult) {
	var CreateTypeGroupAssignmentsOperationResult = function(objectIds) {
        CreateObjectsOperationResult.call(this, objectIds);
    };
	stjs.extend(CreateTypeGroupAssignmentsOperationResult, CreateObjectsOperationResult, [CreateObjectsOperationResult], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.CreateTypeGroupAssignmentsOperationResult';
        prototype.getMessage = function() {
			return "CreateTypeGroupAssignmentsOperationResult";
		};
	}, {});
	return CreateTypeGroupAssignmentsOperationResult;
})