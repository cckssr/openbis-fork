define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
	var ImagingSemanticAnnotation = function() {
	};
	stjs.extend(ImagingSemanticAnnotation, null, [], function(constructor, prototype) {
		prototype['@type'] = 'imaging.dto.ImagingSemanticAnnotation';
		constructor.serialVersionUID = 1;
		prototype.ontologyId = null;
		prototype.ontologyVersion = null;
		prototype.ontologyAnnotationId = null;

		prototype.getOntologyId = function() {
			return this.ontologyId;
		};
		prototype.setOntologyId = function(ontologyId) {
			this.ontologyId = ontologyId;
		};
		prototype.getOntologyVersion = function() {
			return this.ontologyVersion;
		};
		prototype.setOntologyVersion = function(ontologyVersion) {
			this.ontologyVersion = ontologyVersion;
		};
		prototype.getOntologyAnnotationId = function() {
			return this.ontologyAnnotationId;
		};
		prototype.setOntologyAnnotationId = function(ontologyAnnotationId) {
			this.ontologyAnnotationId = ontologyAnnotationId;
		};

		prototype.toString = function() {
            return `(${ontologyId}, ${ontologyVersion}, ${ontologyAnnotationId})`;
        };

	}, {

	});
	return ImagingSemanticAnnotation;
})