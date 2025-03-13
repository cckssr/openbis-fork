/**
 * @author pkupczyk
 */
define([ "stjs", "as/dto/common/update/FieldUpdateValue" ], function(stjs, FieldUpdateValue) {
	var PhysicalDataUpdate = function() {
		this.fileFormatTypeId = new FieldUpdateValue();
		this.archivingRequested = new FieldUpdateValue();
		this.presentInArchive = new FieldUpdateValue();
		this.status = new FieldUpdateValue();
		this.shareId = new FieldUpdateValue();
		this.size = new FieldUpdateValue();
	};
	stjs.extend(PhysicalDataUpdate, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.dataset.update.PhysicalDataUpdate';
		constructor.serialVersionUID = 1;

		/*
		 * @Deprecated
		 */
		prototype.fileFormatTypeId = null;
		prototype.archivingRequested = null;
		prototype.presentInArchive = null;
		prototype.status = null;
		prototype.shareId = null;
		prototype.size = null;

		/*
		 * @Deprecated
		 */
		prototype.getFileFormatTypeId = function() {
			return this.fileFormatTypeId;
		};
		/*
		 * @Deprecated
		 */
		prototype.setFileFormatTypeId = function(fileFormatTypeId) {
			this.fileFormatTypeId.setValue(fileFormatTypeId);
		};
		prototype.isArchivingRequested = function() {
			return this.archivingRequested;
		};
		prototype.setArchivingRequested = function(archivingRequested) {
			this.archivingRequested.setValue(archivingRequested);
		};
		prototype.isPresentInArchive = function() {
			return this.presentInArchive;
		};
		prototype.setPresentInArchive = function(presentInArchive) {
			this.presentInArchive.setValue(presentInArchive);
		};
		prototype.getStatus = function() {
			return this.status;
		};
		prototype.setStatus = function(status) {
			this.status.setValue(status);
		};
		prototype.getShareId = function() {
			return this.shareId;
		};
		prototype.setShareId = function(shareId) {
			this.shareId.setValue(shareId);
		};
        prototype.getSize = function() {
            return this.size;
        };
        prototype.setSize = function(size) {
            this.size.setValue(size);
        };
	}, {
		fileFormatTypeId : {
			name : "FieldUpdateValue",
			arguments : [ "IFileFormatTypeId" ]
		},
		archivingRequested : {
			name : "FieldUpdateValue",
			arguments : [ "Boolean" ]
		},
		presentInArchive : {
			name : "FieldUpdateValue",
			arguments : [ "Boolean" ]
		},
		status : {
			name : "FieldUpdateValue",
			arguments : [ "ArchivingStatus" ]
		},
		shareId : {
			name : "FieldUpdateValue",
			arguments : [ "String" ]
		},
		size : {
			name : "FieldUpdateValue",
			arguments : [ "Long" ]
		}
	});
	return PhysicalDataUpdate;
})