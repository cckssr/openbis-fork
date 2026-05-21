/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/create/CreateObjectsOperationResult" ], function(stjs, CreateObjectsOperationResult) {
	var CreateDataStoresOperationResult = function(objectIds) {
		CreateObjectsOperationResult.call(this, objectIds);
	};
	stjs.extend(CreateDataStoresOperationResult, CreateObjectsOperationResult, [ CreateObjectsOperationResult ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.datastore.create.CreateDataStoresOperationResult';
		prototype.getMessage = function() {
			return "CreateDataStoresOperationResult";
		};
	}, {});
	return CreateDataStoresOperationResult;
})