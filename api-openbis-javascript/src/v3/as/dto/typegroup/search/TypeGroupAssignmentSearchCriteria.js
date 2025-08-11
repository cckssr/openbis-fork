define([ "require", "stjs", "as/dto/common/search/AbstractObjectSearchCriteria", "as/dto/typegroup/search/TypeGroupSearchCriteria",
		"as/dto/common/search/CodeSearchCriteria",
		"as/dto/common/search/AbstractCompositeSearchCriteria" ], function(require, stjs, AbstractObjectSearchCriteria) {
	var TypeGroupAssignmentSearchCriteria = function() {
		AbstractObjectSearchCriteria.call(this);
	};
	stjs.extend(TypeGroupAssignmentSearchCriteria, AbstractObjectSearchCriteria, [ AbstractObjectSearchCriteria ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.typegroup.search.TypeGroupAssignmentSearchCriteria';
		constructor.serialVersionUID = 1;
		prototype.withTypeGroup = function() {
			var TypeGroupSearchCriteria = require("as/dto/typegroup/search/TypeGroupSearchCriteria");
			return this.addCriteria(new TypeGroupSearchCriteria());
		};
		prototype.withSampleType = function() {
            var SampleTypeSearchCriteria = require("as/dto/sample/search/SampleTypeSearchCriteria");
            return this.addCriteria(new SampleTypeSearchCriteria());
        };
	}, {
		criteria : {
			name : "Collection",
			arguments : [ "ISearchCriteria" ]
		}
	});
	return TypeGroupAssignmentSearchCriteria;
})