define([ "stjs", "as/dto/deletion/AbstractObjectDeletionOptions" ], function(stjs, AbstractObjectDeletionOptions) {
	var TypeGroupDeletionOptions = function() {
		AbstractObjectDeletionOptions.call(this);
	};
	stjs.extend(TypeGroupDeletionOptions, AbstractObjectDeletionOptions, [ AbstractObjectDeletionOptions ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.delete.TypeGroupDeletionOptions';
		constructor.serialVersionUID = 1;
	}, {});
	return TypeGroupDeletionOptions;
})