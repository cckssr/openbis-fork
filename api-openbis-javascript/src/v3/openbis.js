define([ 'jquery', 'util/Json', 'as/dto/datastore/search/DataStoreSearchCriteria', 'as/dto/datastore/search/DataStoreKind', 'as/dto/datastore/fetchoptions/DataStoreFetchOptions',
	'as/dto/common/search/SearchResult', 'afs'], function(jquery,
		stjsUtil, DataStoreSearchCriteria, DataStoreKind, DataStoreFetchOptions, SearchResult, AfsServer) {
	jquery.noConflict();

	var __private = function() {

		this.ajaxRequest = function(settings) {
			var thisPrivate = this;

			settings.type = "POST";
			settings.processData = false;
			settings.dataType = "json";
			settings.contentType = "application/json";

			var returnType = settings.returnType;
			if (returnType) {
				delete settings.returnType;
			}

			var data = settings.data;
			data["id"] = "1";
			data["jsonrpc"] = "2.0";

			// decycle each parameter separately (jackson does not recognize
			// object ids across different parameters)

			if (data.params && data.params.length > 0) {
				var newParams = [];
				data.params.forEach(function(param) {
					var newParam = stjsUtil.decycle(param);
					newParams.push(newParam);
				});
				data.params = newParams;
			}

			settings.data = JSON.stringify(data);

			var originalSuccess = settings.success || function() {
			};
			var originalError = settings.error || function() {
			};

			var dfd = jquery.Deferred();
			function success(response) {
				if (response.error) {
					//thisPrivate.log("Request failed - data: " + JSON.stringify(settings.data) + ", error: " + JSON.stringify(response.error));
					originalError(response.error);
					dfd.reject(response.error);
				} else {
					//thisPrivate.log("Request succeeded - data: " + JSON.stringify(settings.data));
					stjsUtil.fromJson(returnType, response.result).done(function(dtos) {
						originalSuccess(dtos);
						dfd.resolve(dtos);
					}).fail(function() {
						originalError(arguments);
						dfd.reject(arguments);
					});
				}
			}

			function error(xhr, status, error) {
				//thisPrivate.log("Request failed - data: " + JSON.stringify(settings.data) + ", error: " + JSON.stringify(error));
				originalError(error);
				dfd.reject(error);
			}

			jquery.ajax(settings).done(success).fail(error);

			return dfd.promise();
		};

		this.ajaxRequestTransactional = function(transactionParticipantId, settings) {
		    if (this.transactionId) {
		        var transactionalSettings = {
                    url : this.transactionCoordinatorUrl,
                    data : {
                        "method" : "executeOperation",
                        "params" : [ this.transactionId,
                                     this.sessionToken,
                                     this.interactiveSessionKey,
                                     transactionParticipantId,
                                     settings.data.method,
                                     settings.data.params ]
                    },
                    returnType : settings.returnType
		        }
                return this.ajaxRequest(transactionalSettings)
		    } else {
		        return this.ajaxRequest(settings)
		    }
		}

		this.loginCommon = function(user, isAnonymousUser, response) {
			var thisPrivate = this;
			var dfd = jquery.Deferred();

			response.done(function(sessionToken) {
				if (sessionToken && (isAnonymousUser || sessionToken.indexOf(user) > -1)) {
					thisPrivate.sessionToken = sessionToken;
					dfd.resolve(sessionToken);
				} else {
					dfd.reject();
				}
			}).fail(function() {
				dfd.reject();
			});
			return dfd.promise();
		};

		this.checkSessionTokenExists = function(){
		    if (!this.sessionToken)
            {
                throw new Error("Session token hasn't been set");
            }
		}

		this.checkInteractiveSessionKeyExists = function(){
            if (!this.interactiveSessionKey)
            {
                throw new Error("Interactive session token hasn't been set");
            }
		}

        this.checkTransactionDoesNotExist = function(){
            if (this.transactionId){
                throw new Error("Operation cannot be executed. Expected no active transactions, but found transaction '" + this.transactionId + "'.");
            }
        }

        this.checkTransactionExists = function(){
            if (!this.transactionId){
                throw new Error("Operation cannot be executed. No active transaction found.");
            }
        }

		this.log = function(msg) {
			if (console) {
				console.log(msg);
			}
		}
	}

	var dataStoreFacade = function(facade, dataStoreCodes) {

		this._getDataStores = function() {
			if (this._dataStores) {
				var dfd = jquery.Deferred();
				dfd.resolve(this._dataStores);
				return dfd.promise();
			} else {
				var thisFacade = this;

				var criteria = new DataStoreSearchCriteria();
				criteria.withKind().thatIn([DataStoreKind.DSS]);
				if (dataStoreCodes.length > 0) {
					criteria.withCodes().thatIn(dataStoreCodes)
				}

				return facade.searchDataStores(criteria, new DataStoreFetchOptions()).then(function(results) {
					var dataStores = results.getObjects();
					var dfd = jquery.Deferred();

					if (dataStores && dataStores.length > 0) {
						thisFacade._dataStores = dataStores;
						dfd.resolve(dataStores);
					} else {
						if (dataStoreCodes.length > 0) {
							dfd.reject("No data stores found for codes: " + dataStoreCodes);
						} else {
							dfd.reject("No data stores found");
						}
					}

					return dfd.promise();
				});
			}
		}

		function createUrlWithParameters(dataStore, servlet, parameters) {
			return dataStore.downloadUrl + "/datastore_server/" + servlet + parameters;
		}

		function createUrl(dataStore) {
			return dataStore.downloadUrl + "/datastore_server/rmi-data-store-server-v3.json";
		}

		function checkTransactionsNotSupported(){
		    if(facade._private.transactionId){
		        throw Error("Transactions are not supported for data store methods.");
		    }
		}

		this.searchFiles = function(criteria, fetchOptions) {
		    checkTransactionsNotSupported()

			var thisFacade = this;
			return this._getDataStores().then(function(dataStores) {
				var promises = dataStores.map(function(dataStore) {
					return facade._private.ajaxRequest({
						url : createUrl(dataStore),
						data : {
							"method" : "searchFiles",
							"params" : [ facade._private.sessionToken, criteria, fetchOptions ]
						},
						returnType : "SearchResult"
					});
				});

				return jquery.when.apply(jquery, promises).then(function() {
					var objects = [];
					var totalCount = 0;

					for (var i = 0; i < arguments.length; i++) {
						var result = arguments[i];

						if (result.getObjects()) {
							Array.prototype.push.apply(objects, result.getObjects());
						}
						if (result.getTotalCount()) {
							totalCount += result.getTotalCount();
						}
					}

					var combinedResult = new SearchResult();
					combinedResult.setObjects(objects);
					combinedResult.setTotalCount(totalCount);
					return combinedResult;
				});
			});
		}

		this.createDataSets = function(creations) {
		    checkTransactionsNotSupported()

			var thisFacade = this;
			var creationsByStore = {};
			for (var i = 0; i < creations.length; i++) {
				var creation = creations[i];
				var dataStoreCode = creation.getMetadataCreation().getDataStoreId().toString();
				if (dataStoreCode in creationsByStore) {
					creationsByStore[dataStoreCode].append(creation);
				} else {
					creationsByStore[dataStoreCode] = [ creation ];
				}
			}
			return this._getDataStores().then(function(dataStores) {
				var promises = [];
				for (var i = 0; i < dataStores.length; i++) {
					var dataStore = dataStores[i];
					var dsCode = dataStore.getCode();
					if (dsCode in creationsByStore) {
						promises.push(facade._private.ajaxRequest({
							url : createUrl(dataStore),
							data : {
								"method" : "createDataSets",
								"params" : [ facade._private.sessionToken, creationsByStore[dsCode] ]
							},
							returnType : {
								name : "List",
								arguments : [ "DataSetPermId" ]
							}
						}));
					}
				}
				return jquery.when.apply(jquery, promises).then(function() {
					var dataSetIds = [];
					for (var i = 0; i < arguments.length; i++) {
						dataSetIds = jquery.merge(dataSetIds, arguments[i]);
					}
					return dataSetIds;
				});

			});
		}

		this.createDataSetUpload = function(dataSetType) {
		    checkTransactionsNotSupported()

			var pad = function(value, length) {
				var result = "" + value;
				while (result.length < length) {
					result = "0" + result;
				}
				return result;
			}

			return this._getDataStores().then(
					function(dataStores) {
						var dfd = jquery.Deferred();

						if (dataStores.length > 1) {
							dfd.reject("Please specify exactly one data store");
						} else {
							var dataStore = dataStores[0];
							var now = new Date();
							var id = "upload-" + now.getFullYear() + pad(now.getMonth() + 1, 2) + pad(now.getDate(), 2) + pad(now.getHours(), 2) + pad(now.getMinutes(), 2) + pad(now.getSeconds(), 2)
									+ "-" + pad(Math.round(Math.random() * 100000), 5);

							dfd.resolve({
								"getId" : function() {
									return id;
								},
								"getUrl" : function(folderPath, ignoreFilePath) {
									var params = {
										"sessionID" : facade._private.sessionToken,
										"uploadID" : id,
										"dataSetType" : dataSetType
									};

									if (folderPath != null) {
										params["folderPath"] = folderPath;
									}

									if (ignoreFilePath != null) {
										params["ignoreFilePath"] = ignoreFilePath;
									}

									return dataStore.downloadUrl + "/datastore_server/store_share_file_upload?" + jquery.param(params);
								},
								"getDataSetType" : function() {
									return dataSetType;
								}
							});
						}

						return dfd.promise();
					});
		}

		this.createUploadedDataSet = function(creation) {
		    checkTransactionsNotSupported()

			var dfd = jquery.Deferred();
			this._getDataStores().done(function(dataStores) {
				if (dataStores.length === 1) {
					facade._private.ajaxRequest({
						url: createUrl(dataStores[0]),
						data: {
							"method": "createUploadedDataSet",
							"params": [facade._private.sessionToken, creation]
						},
						returnType: {
							name: "DataSetPermId"
						}
					}).done(function (response) {
						dfd.resolve(response);
					}).fail(function (error) {
						dfd.reject(error);
					});
				} else {
					dfd.reject("Please specify exactly one data store");
				}
			});
			return dfd.promise();
		}

		this.executeCustomDSSService = function(serviceId, options) {
		    checkTransactionsNotSupported()

		    var dfd = jquery.Deferred();
            this._getDataStores().done(function(dataStores) {
                if (dataStores.length === 1) {
                    facade._private.ajaxRequest({
                        url: createUrl(dataStores[0]),
                        data: {
                            "method": "executeCustomDSSService",
                            "params": [facade._private.sessionToken, serviceId, options]
                        }
                    }).done(function (response) {
                        dfd.resolve(response);
                    }).fail(function (error) {
                        dfd.reject(error);
                    });
                } else {
                    dfd.reject("Please specify exactly one data store");
                }
            });
            return dfd.promise();
		}

		function getUUID() {
			return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
				(c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
			);
		}

	    this.uploadFilesWorkspaceDSS = function(files) {
	        checkTransactionsNotSupported()

			var thisFacade = this;
			var uploadId = getUUID();
			var dfd = jquery.Deferred();

			this._uploadFileWorkspaceDSSEmptyDir(uploadId).then(function() {
				thisFacade._uploadFilesWorkspaceDSS(files, uploadId).then(function(result) {
					dfd.resolve(result);
				}).catch(function(error) {
					dfd.reject(error);
				});
			}).catch(function(error) {
				dfd.reject(error);
			});

			return dfd;
    	}

		this._uploadFilesWorkspaceDSS = async function(files, parentId) {
			var createdDirectories = new Set();
			var filesCount = files.length;
			for (var i = 0; i < filesCount; i++) {
				var relativePath = files[i].webkitRelativePath;
				var directoryRelativePath = relativePath.substring(0, relativePath.lastIndexOf("/") + 1);
				if (directoryRelativePath && !createdDirectories.has(directoryRelativePath)) {
					await this._uploadFileWorkspaceDSSEmptyDir(parentId + "/" + directoryRelativePath);
					createdDirectories.add(directoryRelativePath);
				}
				await this._uploadFileWorkspaceDSSFile(files[i], parentId);
			}
			return parentId;
		}

		this._uploadFileWorkspaceDSSEmptyDir = function(pathToDir) {
			var thisFacade = this;
			var sessionID = facade._private.sessionToken;
			var filename = encodeURIComponent(pathToDir);
			return new Promise(function(resolve, reject) {
				thisFacade._getDataStores().done(function(dataStores) {
					if (dataStores.length === 1) {
						fetch(createUrlWithParameters(dataStores[0], "session_workspace_file_upload",
							"?sessionID=" + sessionID +
							"&filename=" + filename +
							"&id=1&startByte=0&endByte=0&size=0&emptyFolder=true"), {
							method: "POST",
							headers: {
								"Content-Type": "multipart/form-data"
							}
						}).then(function (response) {
							resolve(response);
						}).catch(function (error) {
							reject(error);
						});
					} else {
						reject("Please specify exactly one data store");
					}
				}).fail(function(error) {
					reject(error);
				});
			});
		}

		this._uploadFileWorkspaceDSSFile = function(file, parentId) {
			var thisFacade = this;
			return new Promise(function(resolve, reject) {
				thisFacade._getDataStores().done(function(dataStores) {
					uploadBlob(dataStores[0], parentId, facade._private.sessionToken, file, 0, 1048576)
						.then(function (value) {
							resolve(value);
						})
						.catch(function (reason) {
							reject(reason);
						});
				}).fail(function(error) {
					reject(error);
				});
			});
		}

		async function uploadBlob(dataStore, parentId, sessionID, file, startByte, chunkSize) {
			var fileSize = file.size;
			for (var byte = startByte; byte < fileSize; byte += chunkSize) {
				await fetch(createUrlWithParameters(dataStore, "session_workspace_file_upload",
					"?sessionID=" + sessionID +
					"&filename=" + encodeURIComponent(parentId + "/" +
						(file.webkitRelativePath ? file.webkitRelativePath : file.name)) +
					"&id=1&startByte=" + byte +
					"&endByte=" + (byte + chunkSize) +
					"&size=" + fileSize +
					"&emptyFolder=false"), {
					method: "POST",
					headers: {
						"Content-Type": "multipart/form-data"
					},
					body: makeChunk(file, byte, Math.min(byte + chunkSize, fileSize))
				});
			}
		}

		function makeChunk(file, startByte, endByte) {
			var blob = undefined;
			if (file.slice) {
				blob = file.slice(startByte, endByte);
			} else if (file.webkitSlice) {
				blob = file.webkitSlice(startByte, endByte);
			} else if (file.mozSlice) {
				blob = file.mozSlice(startByte, endByte);
			}
			return blob;
		}
	}

    var AfsServerFacade = function(asFacade) {

        if(!asFacade._private.afsUrl){
            throw Error("Please specify AFS server url");
        }

        var afsServer = new AfsServer(asFacade._private.afsUrl);
        var afsServerTransactionParticipantId = "afs-server"

		this.list = function(owner, source, recursively){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "list",
                        "params" : [ owner, source, recursively ]
                    }
                }).then(function(response){
                    if (response && Array.isArray(response)) {
                        return response.map(function(fileObject){
                            return new File(fileObject)
                        });
                    } else {
                        return AfsServer.prototype.Private.FileEncoderDecoder.decodeFiles(response);
                    }
                })
		    }else{
		        afsServer.useSession(asFacade._private.sessionToken)
				const {promise} =  afsServer.list(owner, source, recursively);
				return promise; 
		    }
		}

        this.read = function(owner, source, offset, limit) {
            return this._read([new AfsServer.prototype.Private.Chunk(owner, source, offset, limit, AfsServer.prototype.Private.ChunkEncoderDecoder.EMPTY_ARRAY)]);
        }

		this._read = function(chunks) {
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "read",
                        "params" : [ Array.from(AfsServer.prototype.Private.ChunkEncoderDecoder.encodeChunks(chunks)) ]
                    }
                }).then(function(chunksAsString){
                    var chunks = AfsServer.prototype.Private.ChunkEncoderDecoder.decodeChunks(chunksAsString); // Decode Chunks
                    var data = chunks[0].getData();
                    return new Blob([data]);
                });
            }else{
                afsServer.useSession(asFacade._private.sessionToken)               
				const {promise} =  afsServer._read(chunks);
				return promise; 
            }
		}

        this.write = function(owner, source, offset, data){
            return this._write([new AfsServer.prototype.Private.Chunk(owner, source, offset, data.length, data)]);
        }

		this._write = function(chunks){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "write",
		                // use base64 url version of encoding that produces url safe characters only (default version of base64 produces "+" and "/" which need to be further converted by encodeURIComponent to "%2B" and "%2F" and therefore they unnecessarily increase the request size)

                        "params" : [ Array.from(AfsServer.prototype.Private.ChunkEncoderDecoder.encodeChunks(chunks)) ]
                    }
                })
            }else{
                afsServer.useSession(asFacade._private.sessionToken)
                const {promise} = afsServer._write(chunks);
				return promise;
            }
		}

		this.delete = function(owner, source){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "delete",
                        "params" : [ owner, source ]
                    }
                })
            }else{
                afsServer.useSession(asFacade._private.sessionToken)
				const {promise} = afsServer.delete(owner, source);
				return promise;                
            }
		}

		this.copy = function(sourceOwner, source, targetOwner, target){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "copy",
                        "params" : [ sourceOwner, source, targetOwner, target ]
                    }
                })
            }else{
                afsServer.useSession(asFacade._private.sessionToken)
                const {promise} = afsServer.copy(sourceOwner, source, targetOwner, target);				 
				return promise;  
            }
        }

		this.move = function(sourceOwner, source, targetOwner, target){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "move",
                        "params" : [ sourceOwner, source, targetOwner, target ]
                    }
                })
            }else{
                afsServer.useSession(asFacade._private.sessionToken)
				const {promise} = afsServer.move(sourceOwner, source, targetOwner, target);				 
				return promise; 
            }
		}

		this.create = function(owner, source, directory){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "create",
                        "params" : [ owner, source, directory ]
                    }
                })
            }else{
                afsServer.useSession(asFacade._private.sessionToken)
				const {promise} = afsServer.create(owner, source, directory);
				return promise; 
            }
		}

		this.free = function(owner, source){
		    if(asFacade._private.transactionId){
                return asFacade._private.ajaxRequestTransactional(afsServerTransactionParticipantId, {
                    data : {
                        "method" : "free",
                        "params" : [ owner, source ]
                    }
                }).then(function(response){
                    if (response) {
                        return new FreeSpace(response)
                    } else {
                        return response;
                    }
                })
            }else{
                afsServer.useSession(asFacade._private.sessionToken)
				const {promise} = afsServer.free(owner, source);
				return promise; 
            }
		}

	}

	var facade = function(asUrl, afsUrl) {

        var openbisUrl = "/openbis/openbis/rmi-application-server-v3.json";
        var transactionCoordinatorUrl = "/openbis/openbis/rmi-transaction-coordinator.json";
        var transactionParticipantId = "application-server"

        if(asUrl){
            var asUrlParts = parseUri(asUrl)
            if (asUrlParts.protocol && asUrlParts.authority) {
                openbisUrl = asUrlParts.protocol + "://" + asUrlParts.authority + openbisUrl;
                transactionCoordinatorUrl = asUrlParts.protocol + "://" + asUrlParts.authority + transactionCoordinatorUrl;
            }
		}

		this._private = new __private();
		this._private.openbisUrl = openbisUrl
		this._private.transactionCoordinatorUrl = transactionCoordinatorUrl
		this._private.afsUrl = afsUrl

        this.setSessionToken = function(sessionToken) {
            var thisFacade = this;
            thisFacade._private.checkTransactionDoesNotExist();
            thisFacade._private.sessionToken = sessionToken;
        }

        this.getSessionToken = function() {
            var thisFacade = this;
            return thisFacade._private.sessionToken;
        }

		this.getAfsUrl = function() {
			return this._private.afsUrl;
		}

		this.login = function(user, password) {
			var thisFacade = this;
			thisFacade._private.checkTransactionDoesNotExist();
			return thisFacade._private.loginCommon(user, false, thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "login",
					"params" : [ user, password ]
				}
			}));
		}

		this.loginAs = function(user, password, asUserId) {
			var thisFacade = this;
			thisFacade._private.checkTransactionDoesNotExist();
			return thisFacade._private.loginCommon(asUserId, false, thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "loginAs",
					"params" : [ user, password, asUserId ]
				}
			}));
		}

		this.loginAsAnonymousUser = function() {
			var thisFacade = this;
			thisFacade._private.checkTransactionDoesNotExist();
			return thisFacade._private.loginCommon(null, true, thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "loginAsAnonymousUser",
					"params" : []
				}
			}));
		}

		this.loginFromContext = function() {
		    this._private.checkTransactionDoesNotExist();
			this._private.sessionToken = this.getWebAppContext().getSessionId();
		}

		this.logout = function() {
			var thisFacade = this;
			thisFacade._private.checkTransactionDoesNotExist();
			return thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "logout",
					"params" : [ thisFacade._private.sessionToken ]
				}
			}).done(function() {
				thisFacade._private.sessionToken = null;
			});
		}

		this.setInteractiveSessionKey = function(interactiveSessionKey) {
		    this._private.interactiveSessionKey = interactiveSessionKey;
		}

		this.beginTransaction = function() {
		    var thisFacade = this;

            thisFacade._private.checkTransactionDoesNotExist();
            thisFacade._private.checkSessionTokenExists();
            thisFacade._private.checkInteractiveSessionKeyExists();

		    thisFacade._private.transactionId = crypto.randomUUID();

		    return thisFacade._private.ajaxRequest({
                url : transactionCoordinatorUrl,
                data : {
                    "method" : "beginTransaction",
                    "params" : [ thisFacade._private.transactionId, thisFacade._private.sessionToken, thisFacade._private.interactiveSessionKey ]
                }
            }).then(function(){
                return thisFacade._private.transactionId;
            })
		}

		this.commitTransaction = function(){
		    var thisFacade = this;

		    thisFacade._private.checkTransactionExists();
            thisFacade._private.checkSessionTokenExists();
            thisFacade._private.checkInteractiveSessionKeyExists();

		    return thisFacade._private.ajaxRequest({
                url : transactionCoordinatorUrl,
                data : {
                    "method" : "commitTransaction",
                    "params" : [ thisFacade._private.transactionId, thisFacade._private.sessionToken, thisFacade._private.interactiveSessionKey ]
                }
            }).then(function(){
                thisFacade._private.transactionId = null;
            });
		}

        this.rollbackTransaction = function(){
            var thisFacade = this;

		    thisFacade._private.checkTransactionExists();
            thisFacade._private.checkSessionTokenExists();
            thisFacade._private.checkInteractiveSessionKeyExists();

		    return thisFacade._private.ajaxRequest({
                url : transactionCoordinatorUrl,
                data : {
                    "method" : "rollbackTransaction",
                    "params" : [ thisFacade._private.transactionId, thisFacade._private.sessionToken, thisFacade._private.interactiveSessionKey ]
                }
            }).then(function(){
                thisFacade._private.transactionId = null;
            });
        }

		this.getSessionInformation = function() {
			var thisFacade = this;
			return thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "getSessionInformation",
					"params" : [ thisFacade._private.sessionToken ]
				},
				returnType : "SessionInformation"
			});
		}

		this.createSpaces = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createSpaces",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "SpacePermId" ]
				}
			});
		}

		this.createProjects = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createProjects",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "ProjectPermId" ]
				}
			});
		}

		this.createExperiments = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createExperiments",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "ExperimentPermId" ]
				}
			});
		}

		this.createExperimentTypes = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createExperimentTypes",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "EntityTypePermId" ]
				}
			});
		}

        /**
         * @deprecated Use "createExternalDataManagementSystems" method instead.
         */
		this.createExternalDms = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createExternalDataManagementSystems",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "ExternalDmsPermId" ]
				}
			});
		}

        this.createExternalDataManagementSystems = function(creations) {
            var thisFacade = this;
            return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
                url : openbisUrl,
                data : {
                    "method" : "createExternalDataManagementSystems",
                    "params" : [ thisFacade._private.sessionToken, creations ]
                },
                returnType : {
                    name : "List",
                    arguments : [ "ExternalDmsPermId" ]
                }
            });
        }

		this.createSamples = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createSamples",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "SamplePermId" ]
				}
			});
		}

		this.createSampleTypes = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createSampleTypes",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "EntityTypePermId" ]
				}
			});
		}

		this.createDataSetTypes = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createDataSetTypes",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "EntityTypePermId" ]
				}
			});
		}

		this.createDataSets = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createDataSets",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "DataSetPermId" ]
				}
			});
		}

		this.createMaterials = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createMaterials",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "MaterialPermId" ]
				}
			});
		}

		this.createMaterialTypes = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createMaterialTypes",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "EntityTypePermId" ]
				}
			});
		}

		this.createPropertyTypes = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createPropertyTypes",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "PropertyTypePermId" ]
				}
			});
		}

		this.createPlugins = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createPlugins",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "PluginPermId" ]
				}
			});
		}

		this.createVocabularies = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createVocabularies",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "VocabularyPermId" ]
				}
			});
		}

		this.createVocabularyTerms = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createVocabularyTerms",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "VocabularyTermPermId" ]
				}
			});
		}

		this.createTags = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createTags",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "TagPermId" ]
				}
			});
		}

		this.createAuthorizationGroups = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createAuthorizationGroups",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "AuthorizationGroupPermId" ]
				}
			});
		}

		this.createRoleAssignments = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createRoleAssignments",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "RoleAssignmentTechId" ]
				}
			});
		}

		this.createPersons = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createPersons",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "PersonPermId" ]
				}
			});
		}

		this.createSemanticAnnotations = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createSemanticAnnotations",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "SemanticAnnotationPermId" ]
				}
			});
		}

		this.createQueries = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createQueries",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "QueryTechId" ]
				}
			});
		}

		this.createPersonalAccessTokens = function(creations) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createPersonalAccessTokens",
					"params" : [ thisFacade._private.sessionToken, creations ]
				},
				returnType : {
					name : "List",
					arguments : [ "PersonalAccessTokenPermId" ]
				}
			});
		}

		this.updateSpaces = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateSpaces",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateProjects = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateProjects",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateExperiments = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateExperiments",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateExperimentTypes = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateExperimentTypes",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateSamples = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateSamples",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateSampleTypes = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateSampleTypes",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateDataSets = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateDataSets",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateDataSetTypes = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateDataSetTypes",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateMaterials = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateMaterials",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateMaterialTypes = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateMaterialTypes",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateExternalDataManagementSystems = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateExternalDataManagementSystems",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updatePropertyTypes = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updatePropertyTypes",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updatePlugins = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updatePlugins",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateVocabularies = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateVocabularies",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateVocabularyTerms = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateVocabularyTerms",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateTags = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateTags",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateAuthorizationGroups = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateAuthorizationGroups",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updatePersons = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updatePersons",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateOperationExecutions = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateOperationExecutions",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateSemanticAnnotations = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateSemanticAnnotations",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updateQueries = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updateQueries",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.updatePersonalAccessTokens = function(updates) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "updatePersonalAccessTokens",
					"params" : [ thisFacade._private.sessionToken, updates ]
				}
			});
		}

		this.getRights = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getRights",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IObjectId", "Rights" ]
				}
			});
		}

		this.getSpaces = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getSpaces",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "ISpaceId", "Space" ]
				}
			});
		}

		this.getProjects = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getProjects",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IProjectId", "Project" ]
				}
			});
		}

		this.getExperiments = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getExperiments",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IExperimentId", "Experiment" ]
				}
			});
		}

		this.getExperimentTypes = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getExperimentTypes",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IEntityTypeId", "ExperimentType" ]
				}
			});
		}

		this.getSamples = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getSamples",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "ISampleId", "Sample" ]
				}
			});
		}

		this.getSampleTypes = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getSampleTypes",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IEntityTypeId", "SampleType" ]
				}
			});
		}

		this.getDataSets = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getDataSets",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IDataSetId", "DataSet" ]
				}
			});
		}

		this.getDataSetTypes = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getDataSetTypes",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IEntityTypeId", "DataSetType" ]
				}
			});
		}

		this.getMaterials = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getMaterials",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IMaterialId", "Material" ]
				}
			});
		}

		this.getMaterialTypes = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getMaterialTypes",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IEntityTypeId", "MaterialType" ]
				}
			});
		}

		this.getPropertyTypes = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getPropertyTypes",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IPropertyTypeId", "PropertyType" ]
				}
			});
		}

		this.getPlugins = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getPlugins",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IPluginId", "Plugin" ]
				}
			});
		}

		this.getVocabularies = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getVocabularies",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IVocabularyId", "Vocabulary" ]
				}
			});
		}

		this.getVocabularyTerms = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getVocabularyTerms",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IVocabularyTermId", "VocabularyTerm" ]
				}
			});
		}

		this.getTags = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getTags",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "ITagId", "Tag" ]
				}
			});
		}

		this.getAuthorizationGroups = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getAuthorizationGroups",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IAuthorizationGroupId", "AuthorizationGroup" ]
				}
			});
		}

		this.getRoleAssignments = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getRoleAssignments",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IRoleAssignmentId", "RoleAssignment" ]
				}
			});
		}

		this.getPersons = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getPersons",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IRoleAssignmentId", "RoleAssignment" ]
				}
			});
		}

		this.getSemanticAnnotations = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getSemanticAnnotations",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "ISemanticAnnotationId", "SemanticAnnotation" ]
				}
			});
		}

		this.getExternalDataManagementSystems = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getExternalDataManagementSystems",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IExternalDmsId", "ExternalDms" ]
				}
			});
		}

		this.getOperationExecutions = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getOperationExecutions",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IOperationExecutionId", "OperationExecution" ]
				}
			});
		}

		this.getQueries = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getQueries",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IQueryId", "Query" ]
				}
			});
		}

		this.getQueryDatabases = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getQueryDatabases",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IQueryDatabaseId", "QueryDatabase" ]
				}
			});
		}

		this.getPersonalAccessTokens = function(ids, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getPersonalAccessTokens",
					"params" : [ thisFacade._private.sessionToken, ids, fetchOptions ]
				},
				returnType : {
					name : "Map",
					arguments : [ "IPersonalAccessTokenId", "PersonalAccessToken" ]
				}
			});
		}

		this.searchSpaces = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchSpaces",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchProjects = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchProjects",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchExperiments = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchExperiments",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			})
		}

		this.searchExperimentTypes = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchExperimentTypes",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			})
		}

		this.searchSamples = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchSamples",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchSampleTypes = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchSampleTypes",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchDataSets = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchDataSets",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchDataSetTypes = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchDataSetTypes",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchMaterials = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchMaterials",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchMaterialTypes = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchMaterialTypes",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchExternalDataManagementSystems = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchExternalDataManagementSystems",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchPlugins = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchPlugins",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchVocabularies = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchVocabularies",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchVocabularyTerms = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchVocabularyTerms",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchTags = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchTags",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchAuthorizationGroups = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchAuthorizationGroups",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchRoleAssignments = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchRoleAssignments",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchPersons = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchPersons",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchCustomASServices = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchCustomASServices",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchSearchDomainServices = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchSearchDomainServices",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchAggregationServices = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchAggregationServices",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchReportingServices = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchReportingServices",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchProcessingServices = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchProcessingServices",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchObjectKindModifications = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchObjectKindModifications",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchGlobally = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchGlobally",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchOperationExecutions = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchOperationExecutions",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchDataStores = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchDataStores",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchPropertyTypes = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchPropertyTypes",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchPropertyAssignments = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchPropertyAssignments",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchSemanticAnnotations = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchSemanticAnnotations",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchQueries = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchQueries",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchQueryDatabases = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchQueryDatabases",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchPersonalAccessTokens = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchPersonalAccessTokens",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.searchSessionInformation = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchSessionInformation",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : "SearchResult"
			});
		}

		this.deleteSpaces = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteSpaces",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteProjects = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteProjects",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteExperiments = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteExperiments",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				},
				returnType : "IDeletionId"
			});
		}

		this.deleteSamples = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteSamples",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				},
				returnType : "IDeletionId"
			});
		}

		this.deleteDataSets = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteDataSets",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				},
				returnType : "IDeletionId"
			});
		}

		this.deleteMaterials = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteMaterials",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteExternalDataManagementSystems = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteExternalDataManagementSystems",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deletePlugins = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deletePlugins",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deletePropertyTypes = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deletePropertyTypes",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteVocabularies = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteVocabularies",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteVocabularyTerms = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteVocabularyTerms",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteExperimentTypes = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteExperimentTypes",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteSampleTypes = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteSampleTypes",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteDataSetTypes = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteDataSetTypes",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteMaterialTypes = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteMaterialTypes",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteTags = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteTags",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteAuthorizationGroups = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteAuthorizationGroups",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteRoleAssignments = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteRoleAssignments",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteOperationExecutions = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteOperationExecutions",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteSemanticAnnotations = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteSemanticAnnotations",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deleteQueries = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deleteQueries",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deletePersons = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deletePersons",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.deletePersonalAccessTokens = function(ids, deletionOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "deletePersonalAccessTokens",
					"params" : [ thisFacade._private.sessionToken, ids, deletionOptions ]
				}
			});
		}

		this.searchDeletions = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchDeletions",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : {
					name : "List",
					arguments : [ "Deletion" ]
				}
			});
		}

		this.searchEvents = function(criteria, fetchOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "searchEvents",
					"params" : [ thisFacade._private.sessionToken, criteria, fetchOptions ]
				},
				returnType : {
					name : "List",
					arguments : [ "Event" ]
				}
			});
		}

		this.revertDeletions = function(ids) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "revertDeletions",
					"params" : [ thisFacade._private.sessionToken, ids ]
				}
			});
		}

		this.confirmDeletions = function(ids) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "confirmDeletions",
					"params" : [ thisFacade._private.sessionToken, ids ]
				}
			});
		}

		this.executeCustomASService = function(serviceId, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeCustomASService",
					"params" : [ thisFacade._private.sessionToken, serviceId, options ]
				}
			});
		}

		this.executeSearchDomainService = function(options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeSearchDomainService",
					"params" : [ thisFacade._private.sessionToken, options ]
				},
				returnType : "SearchResult"
			});
		}

		this.executeAggregationService = function(serviceId, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeAggregationService",
					"params" : [ thisFacade._private.sessionToken, serviceId, options ]
				},
				returnType : "TableModel"
			});
		}

		this.executeReportingService = function(serviceId, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeReportingService",
					"params" : [ thisFacade._private.sessionToken, serviceId, options ]
				},
				returnType : "TableModel"
			});
		}

		this.executeProcessingService = function(serviceId, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeProcessingService",
					"params" : [ thisFacade._private.sessionToken, serviceId, options ]
				}
			});
		}

		this.executeQuery = function(queryId, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeQuery",
					"params" : [ thisFacade._private.sessionToken, queryId, options ]
				}
			});
		}

		this.executeSql = function(sql, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeSql",
					"params" : [ thisFacade._private.sessionToken, sql, options ]
				}
			});
		}

		this.evaluatePlugin = function(options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "evaluatePlugin",
					"params" : [ thisFacade._private.sessionToken, options ]
				}
			});
		}

		this.archiveDataSets = function(ids, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "archiveDataSets",
					"params" : [ thisFacade._private.sessionToken, ids, options ]
				}
			});
		}

		this.unarchiveDataSets = function(ids, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "unarchiveDataSets",
					"params" : [ thisFacade._private.sessionToken, ids, options ]
				}
			});
		}

		this.lockDataSets = function(ids, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "lockDataSets",
					"params" : [ thisFacade._private.sessionToken, ids, options ]
				}
			});
		}

		this.unlockDataSets = function(ids, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "unlockDataSets",
					"params" : [ thisFacade._private.sessionToken, ids, options ]
				}
			});
		}

		this.executeOperations = function(operations, options) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeOperations",
					"params" : [ thisFacade._private.sessionToken, operations, options ]
				}
			});
		}

		this.getServerInformation = function() {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getServerInformation",
					"params" : [ thisFacade._private.sessionToken ]
				}
			});
		}

		this.getServerPublicInformation = function() {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "getServerPublicInformation",
					"params" : []
				}
			});
		}

		this.createPermIdStrings = function(count) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createPermIdStrings",
					"params" : [ thisFacade._private.sessionToken, count ]
				}
			});
		}

		this.createCodes = function(prefix, entityKind, count) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "createCodes",
					"params" : [ thisFacade._private.sessionToken, prefix, entityKind, count ]
				}
			});
		}

		this.executeImport = function(importData, importOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeImport",
					"params" : [ thisFacade._private.sessionToken, importData, importOptions ]
				}
			});
		}

		this.executeExport = function(exportData, exportOptions) {
			var thisFacade = this;
			return thisFacade._private.ajaxRequestTransactional(transactionParticipantId, {
				url : openbisUrl,
				data : {
					"method" : "executeExport",
					"params" : [ thisFacade._private.sessionToken, exportData, exportOptions ]
				}
			});
		}

        this.isSessionActive = function() {
            var thisFacade = this;
            return thisFacade._private.ajaxRequest({
                url : openbisUrl,
                data : {
                    "method" : "isSessionActive",
                    "params" : [ thisFacade._private.sessionToken ]
                }
            });
        }

		this.getDataStoreFacade = function() {
			var dataStoreCodes = [];
			for (var i = 0; i < arguments.length; i++) {
			    var argument = arguments[i]
			    if(Array.isArray(argument)) {
                    Array.prototype.push.apply(dataStoreCodes, argument)
			    } else {
				    dataStoreCodes.push(argument);
				}
			}
			return new dataStoreFacade(this, dataStoreCodes);
		}

		this.getAfsServerFacade = function() {
            return new AfsServerFacade(this)
		}

		this.getMajorVersion = function() {
			var thisFacade = this;
			return thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "getMajorVersion",
					"params" : []
				}
			})
		}

		this.getMinorVersion = function() {
			var thisFacade = this;
			return thisFacade._private.ajaxRequest({
				url : openbisUrl,
				data : {
					"method" : "getMinorVersion",
					"params" : []
				}
			})
		}

		this.uploadToSessionWorkspace = function(file) {
			//Building Form Data Object for Multipart File Upload
			var formData = new FormData();
			formData.append("sessionKeysNumber", "1");
			formData.append("sessionKey_0", "openbis-file-upload");
			formData.append("openbis-file-upload", file);
			formData.append("keepOriginalFileName", "True");
			formData.append("sessionID", this._private.sessionToken);

			var dfd = jquery.Deferred();

			jquery.ajax({
				type: "POST",
				url: "/openbis/openbis/upload",
				contentType: false,
				processData: false,
				data: formData,
				success: function() {
					dfd.resolve();
				},
				error: function() {
					dfd.reject();
				}
			});

			return dfd.promise();
		}

		/**
		 * =======================
		 * OpenBIS webapp context
		 * =======================
		 *
		 * Provides a context information for webapps that are embedded inside
		 * the OpenBIS UI.
		 *
		 * @class
		 *
		 */
		var openbisWebAppContext = function() {
			this.getWebAppParameter = function(parameterName) {
				var match = location.search.match(RegExp("[?|&]" + parameterName + '=(.+?)(&|$)'));
				if (match && match[1]) {
					return decodeURIComponent(match[1].replace(/\+/g, ' '));
				} else {
					return null;
				}
			}

			this.webappCode = this.getWebAppParameter("webapp-code");
			this.sessionId = this.getWebAppParameter("session-id");
			this.entityKind = this.getWebAppParameter("entity-kind");
			this.entityType = this.getWebAppParameter("entity-type");
			this.entityIdentifier = this.getWebAppParameter("entity-identifier");
			this.entityPermId = this.getWebAppParameter("entity-perm-id");

			this.getWebappCode = function() {
				return this.webappCode;
			}

			this.getSessionId = function() {
				return this.sessionId;
			}

			this.getEntityKind = function() {
				return this.entityKind;
			}

			this.getEntityType = function() {
				return this.entityType;
			}

			this.getEntityIdentifier = function() {
				return this.entityIdentifier;
			}

			this.getEntityPermId = function() {
				return this.entityPermId;
			}

			this.getParameter = function(parameterName) {
				return this.getParameter(parameterName);
			}
		}

		this.getWebAppContext = function() {
			return new openbisWebAppContext();
		}
	}

	/*********
	    DTO
	*********/

    var File = function(fileObject){
        this.owner = fileObject.owner;
        this.path = fileObject.path;
        this.name = fileObject.name;
        this.directory = fileObject.directory;
        this.size = fileObject.size;
        this.lastModifiedTime = fileObject.lastModifiedTime ? Date.parse(fileObject.lastModifiedTime) : null;

        this.getOwner = function(){
            return this.owner;
        }
        this.getPath = function(){
            return this.path;
        }
        this.getName = function(){
            return this.name;
        }
        this.getDirectory = function(){
            return this.directory;
        }
        this.getSize = function(){
            return this.size;
        }
        this.getLastModifiedTime = function(){
            return this.lastModifiedTime;
        }
    }

    var FreeSpace = function(freeSpaceObject){

        this.free = freeSpaceObject.free;
        this.total = freeSpaceObject.total;

        this.getFree = function(){
            return this.free;
        }
        this.getTotal = function(){
            return this.total;
        }

    }

    /*********
       UTILS
    *********/

   var { md5, md5_array } = (function(){

   /* this function is much faster,
         so if possible we use it. Some IEs
         are the only ones I know of that
         need the idiotic second function,
         generated by an if clause.  */
       var add32 = function (a, b) {
           return (a + b) & 0xFFFFFFFF;
       },
           hex_chr = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'];


       function cmn(q, a, b, x, s, t) {
           a = add32(add32(a, q), add32(x, t));
           return add32((a << s) | (a >>> (32 - s)), b);
       }

       function md5cycle(x, k) {
           var a = x[0],
               b = x[1],
               c = x[2],
               d = x[3];

           a += (b & c | ~b & d) + k[0] - 680876936 | 0;
           a  = (a << 7 | a >>> 25) + b | 0;
           d += (a & b | ~a & c) + k[1] - 389564586 | 0;
           d  = (d << 12 | d >>> 20) + a | 0;
           c += (d & a | ~d & b) + k[2] + 606105819 | 0;
           c  = (c << 17 | c >>> 15) + d | 0;
           b += (c & d | ~c & a) + k[3] - 1044525330 | 0;
           b  = (b << 22 | b >>> 10) + c | 0;
           a += (b & c | ~b & d) + k[4] - 176418897 | 0;
           a  = (a << 7 | a >>> 25) + b | 0;
           d += (a & b | ~a & c) + k[5] + 1200080426 | 0;
           d  = (d << 12 | d >>> 20) + a | 0;
           c += (d & a | ~d & b) + k[6] - 1473231341 | 0;
           c  = (c << 17 | c >>> 15) + d | 0;
           b += (c & d | ~c & a) + k[7] - 45705983 | 0;
           b  = (b << 22 | b >>> 10) + c | 0;
           a += (b & c | ~b & d) + k[8] + 1770035416 | 0;
           a  = (a << 7 | a >>> 25) + b | 0;
           d += (a & b | ~a & c) + k[9] - 1958414417 | 0;
           d  = (d << 12 | d >>> 20) + a | 0;
           c += (d & a | ~d & b) + k[10] - 42063 | 0;
           c  = (c << 17 | c >>> 15) + d | 0;
           b += (c & d | ~c & a) + k[11] - 1990404162 | 0;
           b  = (b << 22 | b >>> 10) + c | 0;
           a += (b & c | ~b & d) + k[12] + 1804603682 | 0;
           a  = (a << 7 | a >>> 25) + b | 0;
           d += (a & b | ~a & c) + k[13] - 40341101 | 0;
           d  = (d << 12 | d >>> 20) + a | 0;
           c += (d & a | ~d & b) + k[14] - 1502002290 | 0;
           c  = (c << 17 | c >>> 15) + d | 0;
           b += (c & d | ~c & a) + k[15] + 1236535329 | 0;
           b  = (b << 22 | b >>> 10) + c | 0;

           a += (b & d | c & ~d) + k[1] - 165796510 | 0;
           a  = (a << 5 | a >>> 27) + b | 0;
           d += (a & c | b & ~c) + k[6] - 1069501632 | 0;
           d  = (d << 9 | d >>> 23) + a | 0;
           c += (d & b | a & ~b) + k[11] + 643717713 | 0;
           c  = (c << 14 | c >>> 18) + d | 0;
           b += (c & a | d & ~a) + k[0] - 373897302 | 0;
           b  = (b << 20 | b >>> 12) + c | 0;
           a += (b & d | c & ~d) + k[5] - 701558691 | 0;
           a  = (a << 5 | a >>> 27) + b | 0;
           d += (a & c | b & ~c) + k[10] + 38016083 | 0;
           d  = (d << 9 | d >>> 23) + a | 0;
           c += (d & b | a & ~b) + k[15] - 660478335 | 0;
           c  = (c << 14 | c >>> 18) + d | 0;
           b += (c & a | d & ~a) + k[4] - 405537848 | 0;
           b  = (b << 20 | b >>> 12) + c | 0;
           a += (b & d | c & ~d) + k[9] + 568446438 | 0;
           a  = (a << 5 | a >>> 27) + b | 0;
           d += (a & c | b & ~c) + k[14] - 1019803690 | 0;
           d  = (d << 9 | d >>> 23) + a | 0;
           c += (d & b | a & ~b) + k[3] - 187363961 | 0;
           c  = (c << 14 | c >>> 18) + d | 0;
           b += (c & a | d & ~a) + k[8] + 1163531501 | 0;
           b  = (b << 20 | b >>> 12) + c | 0;
           a += (b & d | c & ~d) + k[13] - 1444681467 | 0;
           a  = (a << 5 | a >>> 27) + b | 0;
           d += (a & c | b & ~c) + k[2] - 51403784 | 0;
           d  = (d << 9 | d >>> 23) + a | 0;
           c += (d & b | a & ~b) + k[7] + 1735328473 | 0;
           c  = (c << 14 | c >>> 18) + d | 0;
           b += (c & a | d & ~a) + k[12] - 1926607734 | 0;
           b  = (b << 20 | b >>> 12) + c | 0;

           a += (b ^ c ^ d) + k[5] - 378558 | 0;
           a  = (a << 4 | a >>> 28) + b | 0;
           d += (a ^ b ^ c) + k[8] - 2022574463 | 0;
           d  = (d << 11 | d >>> 21) + a | 0;
           c += (d ^ a ^ b) + k[11] + 1839030562 | 0;
           c  = (c << 16 | c >>> 16) + d | 0;
           b += (c ^ d ^ a) + k[14] - 35309556 | 0;
           b  = (b << 23 | b >>> 9) + c | 0;
           a += (b ^ c ^ d) + k[1] - 1530992060 | 0;
           a  = (a << 4 | a >>> 28) + b | 0;
           d += (a ^ b ^ c) + k[4] + 1272893353 | 0;
           d  = (d << 11 | d >>> 21) + a | 0;
           c += (d ^ a ^ b) + k[7] - 155497632 | 0;
           c  = (c << 16 | c >>> 16) + d | 0;
           b += (c ^ d ^ a) + k[10] - 1094730640 | 0;
           b  = (b << 23 | b >>> 9) + c | 0;
           a += (b ^ c ^ d) + k[13] + 681279174 | 0;
           a  = (a << 4 | a >>> 28) + b | 0;
           d += (a ^ b ^ c) + k[0] - 358537222 | 0;
           d  = (d << 11 | d >>> 21) + a | 0;
           c += (d ^ a ^ b) + k[3] - 722521979 | 0;
           c  = (c << 16 | c >>> 16) + d | 0;
           b += (c ^ d ^ a) + k[6] + 76029189 | 0;
           b  = (b << 23 | b >>> 9) + c | 0;
           a += (b ^ c ^ d) + k[9] - 640364487 | 0;
           a  = (a << 4 | a >>> 28) + b | 0;
           d += (a ^ b ^ c) + k[12] - 421815835 | 0;
           d  = (d << 11 | d >>> 21) + a | 0;
           c += (d ^ a ^ b) + k[15] + 530742520 | 0;
           c  = (c << 16 | c >>> 16) + d | 0;
           b += (c ^ d ^ a) + k[2] - 995338651 | 0;
           b  = (b << 23 | b >>> 9) + c | 0;

           a += (c ^ (b | ~d)) + k[0] - 198630844 | 0;
           a  = (a << 6 | a >>> 26) + b | 0;
           d += (b ^ (a | ~c)) + k[7] + 1126891415 | 0;
           d  = (d << 10 | d >>> 22) + a | 0;
           c += (a ^ (d | ~b)) + k[14] - 1416354905 | 0;
           c  = (c << 15 | c >>> 17) + d | 0;
           b += (d ^ (c | ~a)) + k[5] - 57434055 | 0;
           b  = (b << 21 |b >>> 11) + c | 0;
           a += (c ^ (b | ~d)) + k[12] + 1700485571 | 0;
           a  = (a << 6 | a >>> 26) + b | 0;
           d += (b ^ (a | ~c)) + k[3] - 1894986606 | 0;
           d  = (d << 10 | d >>> 22) + a | 0;
           c += (a ^ (d | ~b)) + k[10] - 1051523 | 0;
           c  = (c << 15 | c >>> 17) + d | 0;
           b += (d ^ (c | ~a)) + k[1] - 2054922799 | 0;
           b  = (b << 21 |b >>> 11) + c | 0;
           a += (c ^ (b | ~d)) + k[8] + 1873313359 | 0;
           a  = (a << 6 | a >>> 26) + b | 0;
           d += (b ^ (a | ~c)) + k[15] - 30611744 | 0;
           d  = (d << 10 | d >>> 22) + a | 0;
           c += (a ^ (d | ~b)) + k[6] - 1560198380 | 0;
           c  = (c << 15 | c >>> 17) + d | 0;
           b += (d ^ (c | ~a)) + k[13] + 1309151649 | 0;
           b  = (b << 21 |b >>> 11) + c | 0;
           a += (c ^ (b | ~d)) + k[4] - 145523070 | 0;
           a  = (a << 6 | a >>> 26) + b | 0;
           d += (b ^ (a | ~c)) + k[11] - 1120210379 | 0;
           d  = (d << 10 | d >>> 22) + a | 0;
           c += (a ^ (d | ~b)) + k[2] + 718787259 | 0;
           c  = (c << 15 | c >>> 17) + d | 0;
           b += (d ^ (c | ~a)) + k[9] - 343485551 | 0;
           b  = (b << 21 | b >>> 11) + c | 0;

           x[0] = a + x[0] | 0;
           x[1] = b + x[1] | 0;
           x[2] = c + x[2] | 0;
           x[3] = d + x[3] | 0;
       }

       function md5blk(s) {
           var md5blks = [],
               i; /* Andy King said do it this way. */

           for (i = 0; i < 64; i += 4) {
               md5blks[i >> 2] = s.charCodeAt(i) + (s.charCodeAt(i + 1) << 8) + (s.charCodeAt(i + 2) << 16) + (s.charCodeAt(i + 3) << 24);
           }
           return md5blks;
       }

       function md5blk_array(a) {
           var md5blks = [],
               i; /* Andy King said do it this way. */

           for (i = 0; i < 64; i += 4) {
               md5blks[i >> 2] = a[i] + (a[i + 1] << 8) + (a[i + 2] << 16) + (a[i + 3] << 24);
           }
           return md5blks;
       }

       function md51(s) {
           var n = s.length,
               state = [1732584193, -271733879, -1732584194, 271733878],
               i,
               length,
               tail,
               tmp,
               lo,
               hi;

           for (i = 64; i <= n; i += 64) {
               md5cycle(state, md5blk(s.substring(i - 64, i)));
           }
           s = s.substring(i - 64);
           length = s.length;
           tail = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
           for (i = 0; i < length; i += 1) {
               tail[i >> 2] |= s.charCodeAt(i) << ((i % 4) << 3);
           }
           tail[i >> 2] |= 0x80 << ((i % 4) << 3);
           if (i > 55) {
               md5cycle(state, tail);
               for (i = 0; i < 16; i += 1) {
                   tail[i] = 0;
               }
           }

           // Beware that the final length might not fit in 32 bits so we take care of that
           tmp = n * 8;
           tmp = tmp.toString(16).match(/(.*?)(.{0,8})$/);
           lo = parseInt(tmp[2], 16);
           hi = parseInt(tmp[1], 16) || 0;

           tail[14] = lo;
           tail[15] = hi;

           md5cycle(state, tail);
           return state;
       }

       function md51_array(a) {
           var n = a.length,
               state = [1732584193, -271733879, -1732584194, 271733878],
               i,
               length,
               tail,
               tmp,
               lo,
               hi;

           for (i = 64; i <= n; i += 64) {
               md5cycle(state, md5blk_array(a.subarray(i - 64, i)));
           }

           // Not sure if it is a bug, however IE10 will always produce a sub array of length 1
           // containing the last element of the parent array if the sub array specified starts
           // beyond the length of the parent array - weird.
           // https://connect.microsoft.com/IE/feedback/details/771452/typed-array-subarray-issue
           a = (i - 64) < n ? a.subarray(i - 64) : new Uint8Array(0);

           length = a.length;
           tail = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
           for (i = 0; i < length; i += 1) {
               tail[i >> 2] |= a[i] << ((i % 4) << 3);
           }

           tail[i >> 2] |= 0x80 << ((i % 4) << 3);
           if (i > 55) {
               md5cycle(state, tail);
               for (i = 0; i < 16; i += 1) {
                   tail[i] = 0;
               }
           }

           // Beware that the final length might not fit in 32 bits so we take care of that
           tmp = n * 8;
           tmp = tmp.toString(16).match(/(.*?)(.{0,8})$/);
           lo = parseInt(tmp[2], 16);
           hi = parseInt(tmp[1], 16) || 0;

           tail[14] = lo;
           tail[15] = hi;

           md5cycle(state, tail);

           return state;
       }

       function rhex(n) {
           var s = '',
               j;
           for (j = 0; j < 4; j += 1) {
               s += hex_chr[(n >> (j * 8 + 4)) & 0x0F] + hex_chr[(n >> (j * 8)) & 0x0F];
           }
           return s;
       }

       function hex(x) {
           var i;
           for (i = 0; i < x.length; i += 1) {
               x[i] = rhex(x[i]);
           }
           return x.join('');
       }

       // In some cases the fast add32 function cannot be used..
       if (hex(md51('hello')) !== '5d41402abc4b2a76b9719d911017c592') {
           add32 = function (x, y) {
               var lsw = (x & 0xFFFF) + (y & 0xFFFF),
                   msw = (x >> 16) + (y >> 16) + (lsw >> 16);
               return (msw << 16) | (lsw & 0xFFFF);
           };
       }

       function md5(s) {
           return hex(md51(s))
       }

       function md5_array(a) {
           return hex(md51_array(a))
       }

       // ---------------------------------------------------

       /**
        * ArrayBuffer slice polyfill.
        *
        * @see https://github.com/ttaubert/node-arraybuffer-slice
        */

       if (typeof ArrayBuffer !== 'undefined' && !ArrayBuffer.prototype.slice) {
           (function () {
               function clamp(val, length) {
                   val = (val | 0) || 0;

                   if (val < 0) {
                       return Math.max(val + length, 0);
                   }

                   return Math.min(val, length);
               }

               ArrayBuffer.prototype.slice = function (from, to) {
                   var length = this.byteLength,
                       begin = clamp(from, length),
                       end = length,
                       num,
                       target,
                       targetArray,
                       sourceArray;

                   if (to !== undefined) {
                       end = clamp(to, length);
                   }

                   if (begin > end) {
                       return new ArrayBuffer(0);
                   }

                   num = end - begin;
                   target = new ArrayBuffer(num);
                   targetArray = new Uint8Array(target);

                   sourceArray = new Uint8Array(this, begin, num);
                   targetArray.set(sourceArray);

                   return target;
               };
           })();
       }

       return { md5, md5_array };
   })();

    // parseUri 1.2.2 (c) Steven Levithan <stevenlevithan.com> MIT License (see http://blog.stevenlevithan.com/archives/parseuri)

    var parseUri = function(str) {
        var options = {
            strictMode: false,
            key: ["source","protocol","authority","userInfo","user","password","host","port","relative","path","directory","file","query","anchor"],
            q:   {
                name:   "queryKey",
                parser: /(?:^|&)([^&=]*)=?([^&]*)/g
            },
            parser: {
                strict: /^(?:([^:\/?#]+):)?(?:\/\/((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?))?((((?:[^?#\/]*\/)*)([^?#]*))(?:\?([^#]*))?(?:#(.*))?)/,
                loose:  /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/
            }
        };

        var	o   = options,
            m   = o.parser[o.strictMode ? "strict" : "loose"].exec(str),
            uri = {},
            i   = 14;

        while (i--) uri[o.key[i]] = m[i] || "";

        uri[o.q.name] = {};
        uri[o.key[12]].replace(o.q.parser, function ($0, $1, $2) {
            if ($1) uri[o.q.name][$1] = $2;
        });

        return uri;
    }

    /** Helper function to convert string md5Hash into an array. */
    var hex2a = function(hexx) {
        var hex = hexx.toString(); //force conversion
        var str = '';
        for (var i = 0; i < hex.length; i += 2)
            str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
        return str;
    }

    function base64URLEncode(str) {
        const base64Encoded = btoa(str);
        return base64Encoded.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    function unit8ArrayToString(bytes) {
        var string = ""
        for (var i = 0; i < bytes.length; i++) {
            string += String.fromCharCode(bytes[i])
        }
        return string
    }

    return facade;

});