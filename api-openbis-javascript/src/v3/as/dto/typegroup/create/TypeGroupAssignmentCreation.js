define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var TypeGroupAssignmentCreation = function() {
	};
	stjs.extend(TypeGroupAssignmentCreation, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.TypeGroupAssignmentCreation';
		constructor.serialVersionUID = 1;
		prototype.sampleTypeId = null;
		prototype.typeGroupId = null;
        prototype.managedInternally = null;

		prototype.getSampleTypeId = function() {
			return this.sampleTypeId;
		};
		prototype.setSampleTypeId = function(sampleTypeId) {
			this.sampleTypeId = sampleTypeId;
		};
        prototype.getTypeGroupId = function() {
            return this.typeGroupId;
        };
        prototype.setTypeGroupId = function(typeGroupId) {
            this.typeGroupId = typeGroupId;
        };
        prototype.isManagedInternally = function() {
            return this.managedInternally;
        };
        prototype.setManagedInternally = function(managedInternally) {
            this.managedInternally = managedInternally;
        };
        prototype.toString = function () {
            return '[' + this.sampleTypeId + ', ' + this.typeGroupId + ']';
        }
	}, {
	    sampleTypeId : "IEntityTypeId",
	    typeGroupId : "ITypeGroupId"
	});
	return TypeGroupAssignmentCreation;
})