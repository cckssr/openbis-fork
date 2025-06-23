/*
 * Copyright 2014 ETH Zuerich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function ExperimentFormController(mainController, mode, experiment) {
	this._mainController = mainController;
	this._experimentFormModel = new ExperimentFormModel(mode, experiment);
	this._experimentFormView = new ExperimentFormView(this, this._experimentFormModel);

	this.refresh = function() {
        if(this._experimentFormModel.dataSetViewer) {
            mainController.sideMenu.removeSubSideMenu();
            mainController.sideMenu.addSubSideMenu(this._experimentFormView._dataSetViewerContainer, this._experimentFormModel.dataSetViewer);
            this._experimentFormModel.dataSetViewer.refresh();
        }
        this._experimentFormView.refresh();
    }
	
	this.init = function(views) {
		var _this = this;
		mainController.serverFacade.getExperimentType(experiment.experimentTypeCode, function(experimentType) {
        _this._experimentFormModel.experimentType = experimentType;
		require([ "as/dto/experiment/id/ExperimentPermId", "as/dto/sample/id/SampleIdentifier", 
            "as/dto/dataset/id/DataSetPermId", "as/dto/experiment/fetchoptions/ExperimentFetchOptions", "as/dto/dataset/search/DataSetSearchCriteria",
            "as/dto/dataset/fetchoptions/DataSetFetchOptions" ],
            function(ExperimentPermId, SampleIdentifier, DataSetPermId, ExperimentFetchOptions, DataSetSearchCriteria, DataSetFetchOptions) {
				if (experiment.permId) {
					var id = new ExperimentPermId(experiment.permId);
					var fetchOptions = new ExperimentFetchOptions();
					fetchOptions.withProject().withSpace();
                    fetchOptions.withType();
                    fetchOptions.withProperties();

					mainController.openbisV3.getExperiments([ id ], fetchOptions).then(function(map) {
						_this._experimentFormModel.v3_experiment = map[id];

						var expeId = _this._experimentFormModel.v3_experiment.getIdentifier().getIdentifier();
                        var dummySampleId = new SampleIdentifier(IdentifierUtil.createDummySampleIdentifierFromExperimentIdentifier(expeId));
                        var dummyDataSetId = new DataSetPermId(IdentifierUtil.createDummyDataSetIdentifierFromExperimentIdentifier(expeId));

                        var dataSetCriteria = new DataSetSearchCriteria()
                        dataSetCriteria.withExperiment().withPermId().thatEquals(experiment.permId)
                        dataSetCriteria.withoutSample()
                        var dataSetFetchOptions = new DataSetFetchOptions()
                        dataSetFetchOptions.count(0)

                        $.when(
                            mainController.openbisV3.getRights([ id , dummySampleId, dummyDataSetId], null),
                            mainController.openbisV3.searchDataSets(dataSetCriteria, dataSetFetchOptions)
                        ).then(function(rightsByIds, dataSetResult){
                            _this._experimentFormModel.rights = rightsByIds[id];
                            _this._experimentFormModel.sampleRights = rightsByIds[dummySampleId];
                            _this._experimentFormModel.dataSetRights = rightsByIds[dummyDataSetId];
                            _this._experimentFormModel.experimentDataSetCount = dataSetResult.getTotalCount()
                            _this._experimentFormView.repaint(views);
                        }, function(error){
                            Util.showError(error);
                        });
					}, function(error){
					    Util.showError(error);
					});
				} else {
					_this._experimentFormView.repaint(views);
				}
		});
		});
	}
	
	this.isDirty = function() {
		return this._experimentFormModel.isFormDirty;
	}
	
	this._addCommentsWidget = function($container) {
		var commentsController = new CommentsController(this._experimentFormModel.experiment, this._experimentFormModel.mode, this._experimentFormModel);
		if(this._experimentFormModel.mode !== FormMode.VIEW || 
			this._experimentFormModel.mode === FormMode.VIEW && !commentsController.isEmpty()) {
			commentsController.init($container);
			return true;
		} else {
			return false;
		}
	}

	this.createObject = function(objectTypeCode) {
        var _this = this;
        Util.blockUI();
        var identifier = _this._experimentFormModel.experiment.identifier;
        var spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(identifier);
        var projectCode = IdentifierUtil.getProjectCodeFromExperimentIdentifier(identifier);
        if(objectTypeCode) {
            setTimeout(function() {
                var argsMap = {
                    "sampleTypeCode" : objectTypeCode,
                    "spaceCode": spaceCode,
                    "projectCode": projectCode,
                    "experimentIdentifier": identifier
                };
                _this._mainController.changeView("showCreateSamplePage", JSON.stringify(argsMap));
            }, 100);
        } else {
            FormUtil.createNewObject(spaceCode,
                                    projectCode,
                                    identifier);
        }
    }
	
	this.deleteExperiment = function(reason) {
		var _this = this;
		
		mainController.serverFacade.listSamplesForExperiments([this._experimentFormModel.experiment], function(dataSamples) {
			mainController.serverFacade.deleteExperiments([_this._experimentFormModel.experiment.id], reason, function(dataExperiment) {
				Util.unblockUI()
				if(dataExperiment.error) {
					Util.showError(dataExperiment.error.message);
				} else {
                    Util.showSuccess("" + ELNDictionary.getExperimentKindName(_this._experimentFormModel.experiment.experimentTypeCode) + " moved to Trashcan");
					
					//Delete experiment from UI
					mainController.sideMenu.deleteNodeByEntityPermId("EXPERIMENT", _this._experimentFormModel.experiment.permId, true);
				}
			});
		});
	}
	
	this.updateExperiment = function() {
		Util.blockUI();
		
		var experimentType = this._mainController.profile.getExperimentTypeForExperimentTypeCode(this._experimentFormModel.experiment.experimentTypeCode);
		
		//Identification Info (This way of collecting the identifier also works for the creation mode)
		var experimentSpace = IdentifierUtil.getSpaceCodeFromIdentifier(this._experimentFormModel.experiment.identifier);
		var experimentProject = IdentifierUtil.getProjectCodeFromExperimentIdentifier(this._experimentFormModel.experiment.identifier);
		var experimentCode = this._experimentFormModel.experiment.code;
		var experimentIdentifier = IdentifierUtil.getExperimentIdentifier(experimentSpace, experimentProject, experimentCode);
		
		var method = "";
		if(this._experimentFormModel.mode === FormMode.CREATE) {
			method = "insertExperiment";
		} else if(this._experimentFormModel.mode === FormMode.EDIT) {
			method = "updateExperiment";
		}
		
		var parameters = {
				//API Method
				"method" : method,
				//Identification Info
				"experimentType" : this._experimentFormModel.experiment.experimentTypeCode,
				"experimentIdentifier" : experimentIdentifier,
				//Properties
				"experimentProperties" : this._experimentFormModel.experiment.properties
		};
		
		var _this = this;
		
		if(this._mainController.profile.allDataStores.length > 0) {
			this._mainController.serverFacade.createReportFromAggregationService(this._mainController.profile.allDataStores[0].code, parameters, function(response) {
				if(response.error) { //Error Case 1
					Util.showError(response.error.message, function() {Util.unblockUI();});
					_this._experimentFormView.refresh();
				} else if (response.result.columns[1].title === "Error") { //Error Case 2
					var stacktrace = response.result.rows[0][1].value;
					Util.showStacktraceAsError(stacktrace);
					_this._experimentFormView.refresh();
				} else if (response.result.columns[0].title === "STATUS" && response.result.rows[0][0].value === "OK") { //Success Case
					var experimentType = _this._mainController.profile.getExperimentTypeForExperimentTypeCode(_this._experimentFormModel.experiment.experimentTypeCode);
					var experimentTypeDisplayName = experimentType.description;
					if(!experimentTypeDisplayName) {
						experimentTypeDisplayName = _this._experimentFormModel.experiment.experimentTypeCode;
					}
					
					var message = "";
                    var prefix = ELNDictionary.getExperimentKindName(_this._experimentFormModel.experiment.experimentTypeCode);
					if(_this._experimentFormModel.mode === FormMode.CREATE) {
                        message = prefix + " Created.";
					} else if(_this._experimentFormModel.mode === FormMode.EDIT) {
                        message = prefix + " Updated.";
					}
					
					var callbackOk = function() {
						_this._experimentFormModel.isFormDirty = false;
						
						if(_this._experimentFormModel.mode === FormMode.CREATE) {
							_this._mainController.sideMenu.refreshCurrentNode(); //Project
							_this._mainController.tabContent.closeCurrentTab();
						} else if(_this._experimentFormModel.mode === FormMode.EDIT) {
							_this._mainController.sideMenu.refreshNodeParentByPermId("EXPERIMENT", _this._experimentFormModel.experiment.permId);
						}
						
						var isInventory = profile.isInventorySpace(experimentSpace);
						if(isInventory) {
							_this._mainController.changeView("showSamplesPage", encodeURIComponent('["' +
									experimentIdentifier + '",false]'));
						} else {
							_this._mainController.changeView("showExperimentPageFromIdentifier",
									encodeURIComponent('["' + experimentIdentifier + '",false]'));
						}

                        if(_this._experimentFormModel.mode === FormMode.CREATE) {
                            require([ 'as/dto/experiment/id/ExperimentIdentifier', "as/dto/experiment/fetchoptions/ExperimentFetchOptions" ],
                                    function(ExperimentIdentifier, ExperimentFetchOptions) {
                                        var id1 = new ExperimentIdentifier(experimentIdentifier);
                                        var fetchOptions = new ExperimentFetchOptions();
                                        mainController.openbisV3.getExperiments([ id1 ], fetchOptions).done(function(map) {
                                            var exp = map[experimentIdentifier];
                                            _this._mainController.sideMenu.refreshCurrentNode().then(x => {
                                                var nodeId = {type: 'EXPERIMENT', id: exp.permId.permId}
                                                _this._mainController.sideMenu.moveToNodeId(JSON.stringify(nodeId));
                                            });
                                        });
                            });
                        }


						Util.unblockUI();
					}
					
					Util.showSuccess(message, callbackOk);
				} else { //This should never happen
					Util.showError("Unknown Error.", function() {Util.unblockUI();});
				}
				
			});
		} else {
			Util.showError("No DSS available.", function() {Util.unblockUI();});
		}
	}
}