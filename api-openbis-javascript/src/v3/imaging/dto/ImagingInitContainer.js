define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
    var ImagingInitContainer = function() {
    };
    stjs.extend(ImagingInitContainer, null, [], function(constructor, prototype) {
        prototype['@type'] = 'imaging.dto.ImagingInitContainer';
        constructor.serialVersionUID = 1;
        prototype.permId = null;
        prototype.adaptor = null;
        prototype.type = null;
        prototype.error = null;
        prototype.config = null;

        prototype.getPermId = function() {
            return this.permId;
        };
        prototype.setPermId = function(permId) {
            this.permId = permId;
        };
        prototype.getAdaptor = function() {
            return this.adaptor;
        };
        prototype.setAdaptor = function(adaptor) {
            this.adaptor = adaptor;
        };
        prototype.getType = function() {
            return this.type;
        };
        prototype.setType = function(type) {
            this.type = type;
        };
        prototype.getError = function() {
            return this.error;
        };
        prototype.setError = function(error) {
            this.error = error;
        };
        prototype.getConfig = function() {
            return this.config;
        };
        prototype.setConfig = function(config) {
            this.config = config;
        };
        prototype.toString = function() {
            return "ImagingInitContainer: " + this.permId;
        };

    }, {

    });
    return ImagingInitContainer;
})