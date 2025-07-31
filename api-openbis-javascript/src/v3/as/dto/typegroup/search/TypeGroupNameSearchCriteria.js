define([ "stjs", "as/dto/common/search/StringFieldSearchCriteria", "as/dto/common/search/SearchFieldType" ], function(stjs, StringFieldSearchCriteria, SearchFieldType) {
	var TypeGroupNameSearchCriteria = function() {
		StringFieldSearchCriteria.call(this, "name", SearchFieldType.ATTRIBUTE);
	};
	stjs.extend(TypeGroupNameSearchCriteria, StringFieldSearchCriteria, [ StringFieldSearchCriteria ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.TypeGroupNameSearchCriteria';
		constructor.serialVersionUID = 1;
	}, {
		fieldType : {
			name : "Enum",
			arguments : [ "SearchFieldType" ]
		}
	});
	return TypeGroupNameSearchCriteria;
})