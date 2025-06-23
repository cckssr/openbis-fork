/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/entity/AbstractEntityUpdate", "as/dto/common/update/FieldUpdateValue",
    "as/dto/common/update/IdListUpdateValue", "as/dto/attachment/update/AttachmentListUpdateValue",
    "as/dto/common/update/ListUpdateMapValues" ],
    function(stjs, AbstractEntityUpdate, FieldUpdateValue, IdListUpdateValue,
		AttachmentListUpdateValue, ListUpdateMapValues) {
	var ExperimentUpdate = function() {
	    AbstractEntityUpdate.call(this);
		this.properties = {};
		this.projectId = new FieldUpdateValue();
		this.tagIds = new IdListUpdateValue();
		this.attachments = new AttachmentListUpdateValue();
		this.metaData = new ListUpdateMapValues();
	};
	stjs.extend(ExperimentUpdate, AbstractEntityUpdate, [AbstractEntityUpdate], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.experiment.update.ExperimentUpdate';
		constructor.serialVersionUID = 1;
		prototype.experimentId = null;
		prototype.freeze = null;
		prototype.freezeForDataSets = null;
		prototype.freezeForSamples = null;
		prototype.projectId = null;
		prototype.tagIds = null;
		prototype.attachments = null;
		prototype.metaData = null;
		prototype.immutableData = null;

		prototype.getObjectId = function() {
			return this.getExperimentId();
		};
		prototype.getExperimentId = function() {
			return this.experimentId;
		};
		prototype.setExperimentId = function(experimentId) {
			this.experimentId = experimentId;
		};
		prototype.shouldBeFrozen = function() {
			return this.freeze;
		}
		prototype.freeze = function() {
			this.freeze = true;
			this.immutableData = true;
		}
		prototype.shouldBeFrozenForDataSets = function() {
			return this.freezeForDataSets;
		}
		prototype.freezeForDataSets = function() {
			this.freeze = true;
			this.freezeForDataSets = true;
			this.immutableData = true;
		}
		prototype.shouldBeFrozenForSamples = function() {
			return this.freezeForSamples;
		}
		prototype.freezeForSamples = function() {
			this.freeze = true;
			this.freezeForSamples = true;
			this.immutableData = true;
		}
		prototype.setProjectId = function(projectId) {
			this.projectId.setValue(projectId);
		};
		prototype.getProjectId = function() {
			return this.projectId;
		};
		prototype.getTagIds = function() {
			return this.tagIds;
		};
		prototype.getAttachments = function() {
			return this.attachments;
		};
		prototype.getMetaData = function() {
            return this.metaData;
        };
        prototype.isImmutableData = function() {
            return this.immutableData;
        };
        prototype.makeDataImmutable = function() {
            this.immutableData = true;
        };
	}, {
		experimentId : "IExperimentId",
		projectId : {
			name : "FieldUpdateValue",
			arguments : [ "IProjectId" ]
		},
		tagIds : {
			name : "IdListUpdateValue",
			arguments : [ "ITagId" ]
		},
		attachments : "AttachmentListUpdateValue",
        metaData : "ListUpdateMapValues"
	});
	return ExperimentUpdate;
})