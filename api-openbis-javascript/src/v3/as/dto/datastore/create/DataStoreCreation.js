/**
 * @author pkupczyk
 */
define([ "stjs" ], function(stjs) {
	var DataStoreCreation = function() {
	};
	stjs.extend(DataStoreCreation, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.datastore.create.DataStoreCreation';
		constructor.serialVersionUID = 1;
		prototype.code = null;
		prototype.downloadUrl = null;
		prototype.remoteUrl = null;
		prototype.storageUuid = null;

		prototype.getCode = function() {
			return this.code;
		};
		prototype.setCode = function(code) {
			this.code = code;
		};
		prototype.getDownloadUrl = function() {
			return this.downloadUrl;
		};
		prototype.setDownloadUrl = function(downloadUrl) {
			this.downloadUrl = downloadUrl;
		};
		prototype.getRemoteUrl = function() {
			return this.remoteUrl;
		};
		prototype.setRemoteUrl = function(remoteUrl) {
			this.remoteUrl = remoteUrl;
		};
		prototype.getStorageUuid = function() {
			return this.storageUuid;
		};
		prototype.setStorageUuid = function(storageUuid) {
			this.storageUuid = storageUuid;
		};
	}, {
	});
	return DataStoreCreation;
})