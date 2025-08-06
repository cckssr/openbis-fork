define([ "require", "stjs", "as/dto/common/search/AbstractObjectSearchCriteria", "as/dto/typegroup/search/TypeGroupCodeSearchCriteria",
		"as/dto/common/search/AbstractCompositeSearchCriteria" ], function(require, stjs, AbstractObjectSearchCriteria) {
	var TypeGroupSearchCriteria = function() {
		AbstractObjectSearchCriteria.call(this);
	};
	stjs.extend(TypeGroupSearchCriteria, AbstractObjectSearchCriteria, [ AbstractObjectSearchCriteria ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.TypeGroupSearchCriteria';
		constructor.serialVersionUID = 1;
		prototype.withCode = function() {
			var TypeGroupCodeSearchCriteria = require("as/dto/typegroup/search/TypeGroupCodeSearchCriteria");
			return this.addCriteria(new TypeGroupCodeSearchCriteria());
		};
	}, {
		criteria : {
			name : "Collection",
			arguments : [ "ISearchCriteria" ]
		}
	});
	return TypeGroupSearchCriteria;
})