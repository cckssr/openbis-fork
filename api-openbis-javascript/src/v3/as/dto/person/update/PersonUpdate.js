define([ "stjs", "as/dto/common/update/FieldUpdateValue", "as/dto/common/update/IdListUpdateValue",
"as/dto/webapp/update/WebAppSettingsUpdateValue", "as/dto/common/update/ListUpdateMapValues" ], function(stjs, FieldUpdateValue,
		IdListUpdateValue, WebAppSettingsUpdateValue, ListUpdateMapValues) {
	var PersonUpdate = function() {
		this.spaceId = new FieldUpdateValue();
		this.active = new FieldUpdateValue();
		this.expiryDate = new FieldUpdateValue();
		this.metaData = new ListUpdateMapValues();
	};
	stjs.extend(PersonUpdate, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.person.update.PersonUpdate';
		constructor.serialVersionUID = 1;
		prototype.userId = null;
		prototype.spaceId = null;
		prototype.webAppSettings = null;
		prototype.active = null;
		prototype.expiryDate = null;
		prototype.metaData = null;

		prototype.getObjectId = function() {
			return this.getUserId();
		};
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
			this.spaceId.setValue(spaceId);
		};
		prototype.getWebAppSettings = function(webAppId) {
			if (webAppId === undefined) {
				return this.webAppSettings;
			} else {
				if (this.webAppSettings == null) {
					this.webAppSettings = {};
				}

				var updateValue = this.webAppSettings[webAppId];

				if (updateValue == null) {
					updateValue = new WebAppSettingsUpdateValue();
					this.webAppSettings[webAppId] = updateValue;
				}

				return updateValue;
			}
		};
		prototype.isActive = function() {
			return this.active;
		};
		prototype.activate = function() {
			this.active.setValue(true);
		};
		prototype.deactivate = function() {
			this.active.setValue(false);
		};
		prototype.getExpiryDate = function() {
            return this.expiryDate;
        };
        prototype.setExpiryDate = function(expiryDate) {
            this.expiryDate.setValue(expiryDate);
        };
        prototype.getMetaData = function() {
            return this.metaData;
        };
	}, {
		userId : "IPersonId",
		spaceId : {
			name : "FieldUpdateValue",
			arguments : [ "ISpaceId" ]
		},
		webAppSettings : {
			name : "Map",
			arguments : [ "String", "WebAppSettingsUpdateValue" ]
		},
		active : {
			name : "FieldUpdateValue",
			arguments : [ "Boolean" ]
		},
		expiryDate : {
            name : "FieldUpdateValue",
            arguments : [ "Date" ]
        },
        metaData : "ListUpdateMapValues"
	});
	return PersonUpdate;
})