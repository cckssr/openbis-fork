define([ "stjs", "as/dto/common/update/FieldUpdateValue", "as/dto/common/update/ListUpdateMapValues" ], function(stjs, FieldUpdateValue, ListUpdateMapValues) {
	var TypeGroupUpdate = function() {
		this.name = new FieldUpdateValue();
		this.metaData = new ListUpdateMapValues();
	};
	stjs.extend(TypeGroupUpdate, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.update.TypeGroupUpdate';
		constructor.serialVersionUID = 1;
		prototype.typeGroupId = null;
		prototype.name = null;
		prototype.metaData = null;

		prototype.getObjectId = function() {
			return this.getTypeGroupId();
		};
		prototype.getTypeGroupId = function() {
			return this.typeGroupId;
		};
		prototype.setTypeGroupId = function(typeGroupId) {
			this.typeGroupId = typeGroupId;
		};
		prototype.getName = function() {
			return this.name;
		};
		prototype.setName = function(name) {
			this.name.setValue(name);
		};
		prototype.getMetaData = function() {
			return this.metaData;
		};
	}, {
		typeGroupId : "ITypeGroupId",
		name : {
			name : "FieldUpdateValue",
			arguments : [ "String" ]
		},
		metaData : 'ListUpdateMapValues'
	});
	return TypeGroupUpdate;
})