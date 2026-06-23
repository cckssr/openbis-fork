/**
 * @author Franz-Josef Elmer
 */

define([ "stjs", "as/dto/common/Enum" ], function(stjs, Enum) {
    var PluginKind = function() {
        Enum.call(this, [ "JYTHON" ]);
    };
    stjs.extend(PluginKind, Enum, [ Enum ], function(constructor, prototype) {
    }, {});
    return new PluginKind();
})
