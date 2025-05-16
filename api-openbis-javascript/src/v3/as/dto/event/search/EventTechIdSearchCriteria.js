/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/search/NumberFieldSearchCriteria", "as/dto/common/search/SearchFieldType" ], function(stjs, NumberFieldSearchCriteria, SearchFieldType) {
	var EventTechIdSearchCriteria = function() {
		NumberFieldSearchCriteria.call(this, "event_tech_id", SearchFieldType.ATTRIBUTE);
	};
	stjs.extend(EventTechIdSearchCriteria, NumberFieldSearchCriteria, [ NumberFieldSearchCriteria ], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.event.search.EventTechIdSearchCriteria';
		constructor.serialVersionUID = 1;
	}, {
		fieldType : {
			name : "Enum",
			arguments : [ "SearchFieldType" ]
		}
	});
	return EventTechIdSearchCriteria;
})