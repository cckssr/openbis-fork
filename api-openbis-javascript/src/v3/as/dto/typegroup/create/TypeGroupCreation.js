define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var TypeGroupCreation = function() {
	};
	stjs.extend(TypeGroupCreation, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.create.TypeGroupCreation';
		constructor.serialVersionUID = 1;
		prototype.code = null;
		prototype.metaData = null;
        prototype.managedInternally = null;

		prototype.getCode = function() {
			return this.code;
		};
		prototype.setCode = function(code) {
			this.code = code;
		};
        prototype.getMetaData = function() {
            return this.metaData;
        };
        prototype.setMetaData = function(metaData) {
            this.metaData = metaData;
        };
        prototype.isManagedInternally = function() {
            return this.managedInternally;
        };
        prototype.setManagedInternally = function(managedInternally) {
            this.managedInternally = managedInternally;
        };
        prototype.toString = function () {
            return '[' + this.code + ', ' + this.managedInternally + ']';
        }
	}, {
		metaData: {
            name: "Map",
            arguments: ["String", "String"]
        }
	});
	return TypeGroupCreation;
})