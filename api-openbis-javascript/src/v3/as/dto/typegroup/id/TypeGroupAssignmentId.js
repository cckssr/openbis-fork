define([ "stjs", "as/dto/typegroup/id/TypeGroupAssignmentId" ], function(stjs, TypeGroupAssignmentId) {
	var TypeGroupAssignmentId = function(sampleTypeId, typeGroupId) {
		this.sampleTypeId = sampleTypeId;
		this.typeGroupId = typeGroupId;
	};
	stjs.extend(TypeGroupAssignmentId, null, [TypeGroupAssignmentId ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.id.TypeGroupAssignmentId';
		constructor.serialVersionUID = 1;
		prototype.sampleTypeId = null;
        prototype.typeGroupId = null;

        prototype.getSampleTypeId = function() {
            return this.sampleTypeId;
        };
        prototype.getTypeGroupId = function() {
            return this.typeGroupId;
        };
        prototype.toString = function () {
            return '[' + this.sampleTypeId + ', ' + this.typeGroupId + ']';
        }
	}, {
	    sampleTypeId : 'IEntityTypeId',
	    typeGroupId : 'ITypeGroupId'
	});
	return TypeGroupAssignmentId;
})