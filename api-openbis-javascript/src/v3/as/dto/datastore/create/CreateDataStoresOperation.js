/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/create/CreateObjectsOperation" ], function(stjs, CreateObjectsOperation) {
	var CreateDataStoresOperation = function(creations) {
		CreateObjectsOperation.call(this, creations);
	};
	stjs.extend(CreateDataStoresOperation, CreateObjectsOperation, [ CreateObjectsOperation ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.datastore.create.CreateDataStoresOperation';
		prototype.getMessage = function() {
			return "CreateDataStoresOperation";
		};
	}, {});
	return CreateDataStoresOperation;
})