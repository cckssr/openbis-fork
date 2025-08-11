define([ "stjs", "as/dto/common/id/ObjectTechId", "as/dto/typegroup/id/ITypeGroupId" ], function(stjs, ObjectTechId, ITypeGroupId) {
	var TypeGroupTechId = function(techId) {
		ObjectTechId.call(this, techId);
	};
	stjs.extend(TypeGroupTechId, ObjectTechId, [ ObjectTechId, ITypeGroupId ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.id.TypeGroupTechId';
		constructor.serialVersionUID = 1;
	}, {});
	return TypeGroupTechId;
})