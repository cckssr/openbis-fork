define([ "stjs", "as/dto/common/search/StringFieldSearchCriteria", "as/dto/common/search/SearchFieldType" ], function(stjs, StringFieldSearchCriteria, SearchFieldType) {
	var TypeGroupCodeSearchCriteria = function() {
		StringFieldSearchCriteria.call(this, "code", SearchFieldType.ATTRIBUTE);
	};
	stjs.extend(TypeGroupCodeSearchCriteria, StringFieldSearchCriteria, [ StringFieldSearchCriteria ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.TypeGroupCodeSearchCriteria';
		constructor.serialVersionUID = 1;
	}, {
		fieldType : {
			name : "Enum",
			arguments : [ "SearchFieldType" ]
		}
	});
	return TypeGroupCodeSearchCriteria;
})