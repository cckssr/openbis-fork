define([ "stjs" ], function(stjs) {
	var PersonCreation = function() {
	};
	stjs.extend(PersonCreation, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.person.create.PersonCreation';
		constructor.serialVersionUID = 1;
		prototype.userId = null;
		prototype.spaceId = null;
		prototype.expiryDate = null;
        prototype.metaData = null;

		prototype.getUserId = function() {
			return this.userId;
		};
		prototype.setUserId = function(userId) {
			this.userId = userId;
		};
		prototype.getSpaceId = function() {
			return this.spaceId;
		};
		prototype.setSpaceId = function(spaceId) {
			this.spaceId = spaceId;
		};
		prototype.getExpiryDate = function() {
            return this.expiryDate;
        };
        prototype.setExpiryDate = function(expiryDate) {
            this.expiryDate = expiryDate;
        };
        prototype.getMetaData = function() {
            return this.metaData;
        };
        prototype.setMetaData = function(metaData) {
            this.metaData = metaData;
        };
	}, {
		spaceId: "ISpaceId",
		expiryDate : "Date",
		metaData: {
             name: "Map",
             arguments: ["String", "String"]
        }
	});
	return PersonCreation;
})