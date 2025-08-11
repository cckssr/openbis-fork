define([ "stjs", "as/dto/common/id/ObjectPermId", "as/dto/typegroup/id/ITypeGroupId" ], function(stjs, ObjectPermId, ITypeGroupId) {
	var TypeGroupId = function(code) {
		ObjectPermId.call(this, code);
	};
	stjs.extend(TypeGroupId, ObjectPermId, [ ObjectPermId, ITypeGroupId ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.id.TypeGroupId';
		constructor.serialVersionUID = 1;
	}, {});
	return TypeGroupId;
})