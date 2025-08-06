define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var TypeGroup = function() {
	};
	stjs.extend(TypeGroup, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.TypeGroup';
		constructor.serialVersionUID = 1;
		prototype.id = null;
		prototype.fetchOptions = null;

		prototype.code = null;
		prototype.registrator = null;
		prototype.registrationDate = null;
		prototype.modifier = null;
		prototype.modificationDate = null;
		prototype.typeGroupAssignments = null;
		prototype.metaData = null;
        prototype.managedInternally = null;

		prototype.getFetchOptions = function() {
			return this.fetchOptions;
		};
		prototype.setFetchOptions = function(fetchOptions) {
			this.fetchOptions = fetchOptions;
		};
		prototype.getId = function() {
			return this.id;
		};
		prototype.setId = function(id) {
			this.id = id;
		};
		prototype.getCode = function() {
			return this.code;
		};
		prototype.setCode = function(code) {
			this.code = code;
		};
		prototype.getRegistrationDate = function() {
			return this.registrationDate;
		};
		prototype.setRegistrationDate = function(registrationDate) {
			this.registrationDate = registrationDate;
		};
		prototype.getModificationDate = function() {
			return this.modificationDate;
		};
		prototype.setModificationDate = function(modificationDate) {
			this.modificationDate = modificationDate;
		};
		prototype.getRegistrator = function() {
			if (this.getFetchOptions() && this.getFetchOptions().hasRegistrator()) {
				return this.registrator;
			} else {
				throw new exceptions.NotFetchedException("Registrator has not been fetched.");
			}
		};
		prototype.setRegistrator = function(registrator) {
			this.registrator = registrator;
		};
		prototype.getModifier = function() {
            if (this.getFetchOptions() && this.getFetchOptions().hasModifier()) {
                return this.modifier;
            } else {
                throw new exceptions.NotFetchedException("Modifier has not been fetched.");
            }
        };
        prototype.setModifier = function(modifier) {
            this.modifier = modifier;
        };
        prototype.getTypeGroupAssignments = function() {
            if (this.getFetchOptions() && this.getFetchOptions().hasTypeGroupAssignments()) {
                return this.typeGroupAssignments;
            } else {
                throw new exceptions.NotFetchedException("Type group assignments have not been fetched.");
            }
        };
        prototype.setTypeGroupAssignments = function(typeGroupAssignments) {
            this.typeGroupAssignments = typeGroupAssignments;
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
            return this.getName()
        }
	}, {
		fetchOptions : "TypeGroupFetchOptions",
		id : "TypeGroupId",
		registrationDate : "Date",
		modificationDate : "Date",
		registrator : "Person",
		modifier : "Person",
		typeGroupAssignments: {
          name: 'List',
          arguments: ['TypeGroupAssignment']
        },
		metaData: {
            name: "Map",
            arguments: ["String", "String"]
        }
	});
	return TypeGroup;
})