define([ "stjs", "as/dto/common/update/FieldUpdateValue", "as/dto/common/update/ListUpdateMapValues" ], function(stjs, FieldUpdateValue, ListUpdateMapValues) {
	var TypeGroupUpdate = function() {
		this.name = new FieldUpdateValue();
		this.metaData = new ListUpdateMapValues();
	};
	stjs.extend(TypeGroupUpdate, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.update.TypeGroupUpdate';
		constructor.serialVersionUID = 1;
		prototype.typeGroupId = null;
		prototype.code = null;
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
		prototype.getCode = function() {
			return this.code;
		};
		prototype.setCode = function(code) {
			this.code.setValue(code);
		};
		prototype.getMetaData = function() {
			return this.metaData;
		};
	}, {
		typeGroupId : "ITypeGroupId",
		code : {
			name : "FieldUpdateValue",
			arguments : [ "String" ]
		},
		metaData : 'ListUpdateMapValues'
	});
	return TypeGroupUpdate;
})