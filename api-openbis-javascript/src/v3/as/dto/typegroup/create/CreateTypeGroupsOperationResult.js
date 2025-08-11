define([ "stjs", "as/dto/common/create/CreateObjectsOperationResult"], function(stjs, CreateObjectsOperationResult) {
	var CreateTypeGroupsOperationResult = function(objectIds) {
        CreateObjectsOperationResult.call(this, objectIds);
    };
	stjs.extend(CreateTypeGroupsOperationResult, CreateObjectsOperationResult, [CreateObjectsOperationResult], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.CreateTypeGroupsOperationResult';
        prototype.getMessage = function() {
			return "CreateTypeGroupsOperationResult";
		};
	}, {});
	return CreateTypeGroupsOperationResult;
})