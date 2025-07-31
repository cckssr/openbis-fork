define([ "require", "stjs", "as/dto/common/search/AbstractObjectSearchCriteria", "as/dto/typegroup/search/TypeGroupNameSearchCriteria",
		"as/dto/common/search/AbstractCompositeSearchCriteria" ], function(require, stjs, AbstractObjectSearchCriteria) {
	var TypeGroupSearchCriteria = function() {
		AbstractObjectSearchCriteria.call(this);
	};
	stjs.extend(TypeGroupSearchCriteria, AbstractObjectSearchCriteria, [ AbstractObjectSearchCriteria ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.TypeGroupSearchCriteria';
		constructor.serialVersionUID = 1;
		prototype.withName = function() {
			var TypeGroupNameSearchCriteria = require("as/dto/typegroup/search/TypeGroupNameSearchCriteria");
			return this.addCriteria(new TypeGroupNameSearchCriteria());
		};
	}, {
		criteria : {
			name : "Collection",
			arguments : [ "ISearchCriteria" ]
		}
	});
	return TypeGroupSearchCriteria;
})