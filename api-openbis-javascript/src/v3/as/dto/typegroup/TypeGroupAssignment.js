define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var TypeGroupAssignment = function() {
	};
	stjs.extend(TypeGroupAssignment, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.TypeGroupAssignment';
		constructor.serialVersionUID = 1;
		prototype.sampleType = null;
		prototype.typeGroup = null;
		prototype.fetchOptions = null;
		prototype.registrator = null;
		prototype.registrationDate = null;

        prototype.managedInternally = null;

		prototype.getFetchOptions = function() {
			return this.fetchOptions;
		};
		prototype.setFetchOptions = function(fetchOptions) {
			this.fetchOptions = fetchOptions;
		};
		prototype.getSampleType = function() {
			return this.sampleType;
		};
		prototype.setSampleType = function(sampleType) {
			this.sampleType = sampleType;
		};
		prototype.getTypeGroup = function() {
			return this.typeGroup;
		};
		prototype.setTypeGroup = function(typeGroup) {
			this.typeGroup = typeGroup;
		};
		prototype.getRegistrationDate = function() {
			return this.registrationDate;
		};
		prototype.setRegistrationDate = function(registrationDate) {
			this.registrationDate = registrationDate;
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
        prototype.isManagedInternally = function() {
            return this.managedInternally;
        };
        prototype.setManagedInternally = function(managedInternally) {
            this.managedInternally = managedInternally;
        };
        prototype.toString = function () {
            return "TypeGroupAssignment: ["+this.typeGroup + ", " + this.sampleType + ']';
        }
	}, {
	    sampleType : "SampleType",
	    typeGroup : "TypeGroup",
		fetchOptions : "TypeGroupAssignmentFetchOptions",
		registrationDate : "Date",
		registrator : "Person"
	});
	return TypeGroupAssignment;
})