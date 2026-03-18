/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/Enum" ], function(stjs, Enum) {
	var GlobalSearchObjectKind = function() {
		Enum.call(this, [ "EXPERIMENT", "SAMPLE", "DATA_SET" ]);
	};
	stjs.extend(GlobalSearchObjectKind, Enum, [ Enum ], function(constructor, prototype) {
	}, {});
	return new GlobalSearchObjectKind();
})
