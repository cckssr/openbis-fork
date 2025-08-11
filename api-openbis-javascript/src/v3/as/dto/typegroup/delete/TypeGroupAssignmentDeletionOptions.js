define([ "stjs", "as/dto/deletion/AbstractObjectDeletionOptions" ], function(stjs, AbstractObjectDeletionOptions) {
	var TypeGroupAssignmentDeletionOptions = function() {
		AbstractObjectDeletionOptions.call(this);
	};
	stjs.extend(TypeGroupAssignmentDeletionOptions, AbstractObjectDeletionOptions, [ AbstractObjectDeletionOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions';
		constructor.serialVersionUID = 1;
	}, {});
	return TypeGroupAssignmentDeletionOptions;
})