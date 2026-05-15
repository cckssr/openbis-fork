function MoveEntityController(entityType, entityPermIds, optionalPostAction) {
	var moveEntityModel = new MoveEntityModel();
	var moveEntityView = new MoveEntityView(this, moveEntityModel);
	
	var searchAndCallback = function(callback) {
        if (_.isString(entityPermIds)) {
            entityPermIds = entityPermIds.trim()
            var criteria = { entityKind : entityType, logicalOperator : "AND", rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : entityPermIds } } };
        } else {
            var criteria = { entityKind : entityType, logicalOperator : "OR", rules : {} };
            for(var pIdx = 0; pIdx < entityPermIds.length; pIdx++) {
                criteria.rules["UUIDv4_" + pIdx] = { type : "Attribute", name : "PERM_ID", value : entityPermIds[pIdx] };
            }
        }

		switch(entityType) {
			case "EXPERIMENT":
				mainController.serverFacade.searchForExperimentsAdvanced(criteria, null, callback);
				break;
			case "SAMPLE":
				mainController.serverFacade.searchForSamplesAdvanced(criteria, { only : true, withType: true, withExperiment: true, withProject: true, withSpace: true }, callback);
				break;
			case "DATASET":
				mainController.serverFacade.searchForDataSetsAdvanced(criteria, null, callback);
				break;
			case "PROJECT":
				mainController.serverFacade.searchForProjectsAdvanced(criteria, null, callback);
				break;
		}
	};
	
	this.init = function() {
		searchAndCallback(function(result) {
			moveEntityModel.entity = result.objects[0];
			moveEntityModel.entities = result.objects;
			moveEntityView.repaint();
		});
	};
	
	var waitForIndexUpdate = function() {
		searchAndCallback(function(result) {
			var entity = result.objects[0];
			var found = false;
			switch(entityType) {
				case "EXPERIMENT":
					found = entity.getProject().getIdentifier().identifier === moveEntityModel.selected.getIdentifier().identifier;
					break;
				case "SAMPLE":
				    var selectedEntityType = moveEntityModel.selected["@type"]
				    switch(selectedEntityType) {
                        case "as.dto.project.Project":
                            found = entity.getExperiment() == null && entity.getProject().getIdentifier().identifier === moveEntityModel.selected.getIdentifier().identifier;
                            break;
                        case "as.dto.experiment.Experiment":
                            found = entity.getExperiment().getIdentifier().identifier === moveEntityModel.selected.getIdentifier().identifier;
                            break;
                        case "as.dto.space.Space":
                            found = entity.getExperiment() == null && entity.getProject() == null && entity.getSpace().getPermId().permId === moveEntityModel.selected.getPermId().permId;
                            break;
                    }

					break;
				case "DATASET":
					found = (entity.getSample() && entity.getSample().getIdentifier().identifier === moveEntityModel.selected.getIdentifier().identifier)
							||
							(entity.getExperiment() && entity.getExperiment().getIdentifier().identifier === moveEntityModel.selected.getIdentifier().identifier);
					break;
				case "PROJECT":
					found = entity.getSpace().getPermId().identifier === moveEntityModel.selected.getPermId().identifier;
					break;
			}
			
			if(!found) {
				setTimeout(function(){ waitForIndexUpdate(); }, 300);
			} else {
                Util.showSuccess("Moved successfully", async function() {
                    Util.unblockUI();

                    await mainController.sideMenu.refreshNodeParentByPermId(entityType, entity.getPermId().permId); // Refresh old node parent

                    var selectedType = moveEntityModel.selected["@type"]
                    var selectedEntityType = null

                    if(selectedType === "as.dto.space.Space"){
                        selectedEntityType = "SPACE"
                    }else if(selectedType === "as.dto.project.Project"){
                        selectedEntityType = "PROJECT"
                    }else if(selectedType === "as.dto.experiment.Experiment"){
                        selectedEntityType = "EXPERIMENT"
                    }else if(selectedType === "as.dto.sample.Sample"){
                        selectedEntityType = "SAMPLE"
                    }else if(selectedType === "as.dto.dataset.DataSet"){
                        selectedEntityType = "DATASET"
                    }

                    await mainController.sideMenu.refreshNodeByPermId(selectedEntityType, moveEntityModel.selected.getPermId().permId); // New node parent

                    switch(entityType) {
                        case "EXPERIMENT":
                            mainController.changeView("showExperimentPageFromIdentifier",
                                    encodeURIComponent('["' + entity.getIdentifier().identifier + '",false]'));
                            mainController.sideMenu.moveToNodeId(JSON.stringify({type: "EXPERIMENT", id: entity.getPermId().permId}));
                            break;
                        case "SAMPLE":
                            mainController.changeView("showViewSamplePageFromPermId", entity.getPermId().permId);
                            mainController.sideMenu.moveToNodeId(JSON.stringify({type: "SAMPLE", id: entity.getPermId().permId}));
                            break;
                        case "DATASET":
                            mainController.changeView("showViewDataSetPageFromPermId", entity.getPermId().permId);
                            mainController.sideMenu.moveToNodeId(JSON.stringify({type: "DATASET", id: entity.getPermId().permId}));
                            break;
                        case "PROJECT":
                            mainController.changeView("showProjectPageFromIdentifier", entity.getPermId().permId);
                            mainController.sideMenu.moveToNodeId(JSON.stringify({type: "PROJECT", id: entity.getPermId().permId}));
                            break;
                    }
				});
			}
		});
	}
	
	this.move = function(descendants) {
	    var _this = this;
		Util.blockUI();
		
		var done = function() {
			waitForIndexUpdate();
			if(optionalPostAction) {
			    optionalPostAction();
			}
		};

        //only for multiple sample movement
		var doneMultiple = function() {
		    var selectedEntity = moveEntityModel.selected;
		    var selectedEntityType = moveEntityModel.selected["@type"];

		    var callback = async function(resultObject) {
		        var samples = resultObject.objects;
		        samples = samples.map(x => x.permId.permId);
		        var entitiesDone = [];
                switch(selectedEntityType) {
                    case "as.dto.project.Project":
                        var selectedType = "PROJECT";
                        var selectedEntityPermId = selectedEntity.getPermId().getPermId();
                        break;
                    case "as.dto.experiment.Experiment":
                        var selectedType = "EXPERIMENT";
                        var selectedEntityPermId = selectedEntity.getPermId().getPermId();
                        break;
                    case "as.dto.space.Space":
                        var selectedType = "SPACE";
                        var selectedEntityPermId = selectedEntity.getPermId().getPermId();
                        break;
                }
		        for(var i = 0; i < moveEntityModel.entities.length; i++) {
		            var entity = moveEntityModel.entities[i];
		            var permId = entity.permId.permId;
		            if(entitiesDone.includes(permId)) {
		                continue;
		            }
		            //refresh old entities
		            if(entity.getExperiment()) {
		                await mainController.sideMenu.refreshNodeByPermId("EXPERIMENT", entity.getExperiment().getPermId().permId);
		            } else if(entity.getProject()) {
		                await mainController.sideMenu.refreshNodeByPermId("PROJECT", entity.getProject().getPermId().permId);
		            } else {
		                await mainController.sideMenu.refreshNodeByPermId("SPACE", entity.getSpace().getPermId().permId);
		            }
		            if(samples.includes(permId)) {
		                entitiesDone.push(permId);
		            }
		        }
		        await mainController.sideMenu.refreshNodeByPermId(selectedType, selectedEntityPermId);
		        if(descendants) {
		            Util.showSuccess("Moving of " + entitiesDone.length + " objects and their descends has been finished", async function() { Util.unblockUI(); });
		        } else {
		            Util.showSuccess("Moving of " + entitiesDone.length + " objects has been finished", async function() { Util.unblockUI(); });
		        }
		        if(optionalPostAction) {
		            optionalPostAction();
		        }
		    };

		    switch(selectedEntityType) {
                case "as.dto.project.Project":
                    var criteria = {
                                        entityKind : entityType, logicalOperator : "AND",
                                        rules : {
                                            "UUIDv4_0" : { type : "Attribute", name : "PROJECT_PERM_ID", value : selectedEntity.getPermId().permId },
                                            "UUIDv4_1" : { type: "Experiment", name: "NULL.NULL", value: "NULL" }
                                        }
                                   };
                    mainController.serverFacade.searchForSamplesAdvanced(criteria, { only : true }, callback);
                    break;
                case "as.dto.experiment.Experiment":
                    var criteria = { entityKind : entityType, logicalOperator : "AND",
                                        rules : {
                                            "UUIDv4" : { type: "Experiment", name: "ATTR.PERM_ID", value : selectedEntity.getPermId().permId }
                                        }
                                    };
                    mainController.serverFacade.searchForSamplesAdvanced(criteria, { only : true }, callback);
                    break;
                case "as.dto.space.Space":
                    var criteria = {
                                        entityKind : entityType, logicalOperator : "AND",
                                        rules : {
                                            "UUIDv4_0" : { type: "Attribute", name: "SPACE", value : selectedEntity.getPermId().permId },
                                            "UUIDv4_1" : { type: "Project", name: "NULL.NULL", value: "NULL" },
                                            "UUIDv4_2" : { type: "Experiment", name: "NULL.NULL", value: "NULL" }
                                        }
                                    };
                    mainController.serverFacade.searchForSamplesAdvanced(criteria, { only : true }, callback);
                    break;
            }
		};

		var fail = function(error) {
			var msg = JSON.stringify(error);
			if (error && error.data && error.data.message) {
				msg = error.data.message;
			}
			Util.showError("Move failed: " + msg);
		};


        if(moveEntityModel.isNewExperiment && !moveEntityModel.experimentIdentifier) {
            Util.showUserError("Please choose the project and " + ELNDictionary.getExperimentDualName() + " name.", function() {});
            return;
        }

        if(moveEntityModel.isNewExperiment && !moveEntityModel.experimentType) {
            Util.showUserError("Please choose the " + ELNDictionary.getExperimentDualName() + " type.", function() {});
            return;
        }


		var moveEntityFunction = function() {
            switch(entityType) {
                case "EXPERIMENT":
                    require([ "as/dto/experiment/update/ExperimentUpdate"],
                        function(ExperimentUpdate) {
                            var experimentUpdate = new ExperimentUpdate();
                            experimentUpdate.setExperimentId(moveEntityModel.entity.getIdentifier());
                            experimentUpdate.setProjectId(moveEntityModel.selected.getIdentifier());
                            experimentUpdate.setProperties(moveEntityModel.entity.getProperties())
                            mainController.openbisV3.updateExperiments([ experimentUpdate ]).done(done).fail(fail);
                        });
                    break;
                case "SAMPLE":
                    require([ "as/dto/sample/fetchoptions/SampleFetchOptions",
                              "as/dto/sample/update/SampleUpdate", "as/dto/space/id/SpacePermId"],
                        function(SampleFetchOptions, SampleUpdate, SpacePermId) {
                            var doneFunction = moveEntityModel.entities.length === 1 ? done : doneMultiple;

                            var prepareSampleUpdate = function(samplePermId) {
                                var sampleUpdate = new SampleUpdate();
                                sampleUpdate.setSampleId(samplePermId);
                                switch(selectedEntityType) {
                                    case "as.dto.project.Project":
                                        sampleUpdate.setExperimentId(null);
                                        sampleUpdate.setProjectId(moveEntityModel.selected.getPermId())
                                        sampleUpdate.setSpaceId(moveEntityModel.selected.getSpace().getPermId());
                                        break;
                                    case "as.dto.experiment.Experiment":
                                        sampleUpdate.setSpaceId(moveEntityModel.selected.getProject().getSpace().getPermId());
                                        sampleUpdate.setProjectId(moveEntityModel.selected.getProject().getPermId());
                                        sampleUpdate.setExperimentId(moveEntityModel.selected.getPermId())
                                        break;
                                    case "as.dto.space.Space":
                                        sampleUpdate.setExperimentId(null);
                                        sampleUpdate.setProjectId(null);
                                        sampleUpdate.setSpaceId(moveEntityModel.selected.getPermId());
                                        break;
                                }
                                return sampleUpdate;
                            }

                            var permIds = moveEntityModel.entities.map(x => x.getPermId());
                            var selectedEntityType = moveEntityModel.selected["@type"];


                                if (descendants) {
                                    var fetchOptions = new SampleFetchOptions();
                                    fetchOptions.withExperiment();
                                    fetchOptions.withProject();
                                    fetchOptions.withSpace();
                                    fetchOptions.withChildrenUsing(fetchOptions);
                                     mainController.openbisV3.getSamples(permIds, fetchOptions).done(function(map) {
                                        var samplesToUpdate = [];
                                        var updates = [];

                                         for(var i = 0; i < moveEntityModel.entities.length; i++) {
                                            var entity = moveEntityModel.entities[i];
                                            var permId = entity.getPermId();
                                            _this.gatherAllDescendants(samplesToUpdate, map[permId]);

                                            if(entity.getExperiment()) {
                                                var level = "EXPERIMENT";
                                                var currentEntity = entity.getExperiment().getPermId().getPermId();
                                            } else if(entity.getProject()) {
                                                var level = "PROJECT";
                                                var currentEntity = entity.getProject().getPermId().getPermId();
                                            } else {
                                                var level = "SPACE";
                                                var currentEntity = entity.getSpace().getPermId().getPermId();
                                            }

                                            switch(level) {
                                                case "EXPERIMENT":
                                                    samplesToUpdate.forEach(function(sample) {
                                                        if (sample.getExperiment() != null && currentEntity == sample.getExperiment().getPermId().getPermId()) {
                                                            var sampleUpdate = prepareSampleUpdate(sample.getPermId());
                                                            updates.push(sampleUpdate);
                                                        }
                                                    });
                                                    break;
                                                case "PROJECT":
                                                    samplesToUpdate.forEach(function(sample) {
                                                        if (sample.getExperiment() == null && currentEntity == sample.getProject().getPermId().getPermId()) {
                                                            var sampleUpdate = prepareSampleUpdate(sample.getPermId());
                                                            updates.push(sampleUpdate);
                                                        }
                                                    });
                                                    break;
                                                case "SPACE":
                                                    samplesToUpdate.forEach(function(sample) {
                                                        if (sample.getExperiment() == null && sample.getProject() == null && currentEntity == sample.getSpace().getPermId().getPermId()) {
                                                            var sampleUpdate = prepareSampleUpdate(sample.getPermId());
                                                            updates.push(sampleUpdate);
                                                        }
                                                    });
                                                    break;
                                            }
                                        }
                                        mainController.openbisV3.updateSamples(updates).done(doneFunction).fail(fail);
                                    });
                                } else {
                                    var sampleUpdate = permIds.map(x => prepareSampleUpdate(x));
                                    mainController.openbisV3.updateSamples( sampleUpdate ).done(doneFunction).fail(fail);
                                }

                    });


                    break;
                case "DATASET":
                    require([ "as/dto/dataset/update/DataSetUpdate"],
                        function(DataSetUpdate) {
                            var datasetUpdate = new DataSetUpdate();
                            datasetUpdate.setDataSetId(moveEntityModel.entity.getPermId());
                            datasetUpdate.setProperties(moveEntityModel.entity.getProperties())

                            switch(moveEntityModel.selected["@type"]) {
                                case "as.dto.experiment.Experiment":
                                    datasetUpdate.setExperimentId(moveEntityModel.selected.getIdentifier());
                                break;
                                case "as.dto.sample.Sample":
                                    if(moveEntityModel.selected.getExperiment()) {
                                        datasetUpdate.setExperimentId(moveEntityModel.selected.getExperiment().getIdentifier());
                                    }
                                    datasetUpdate.setSampleId(moveEntityModel.selected.getIdentifier());
                                break;
                            }

                            mainController.openbisV3.updateDataSets([ datasetUpdate ]).done(done).fail(fail);
                        });
                    break;
                case "PROJECT":
                    require(["as/dto/project/update/ProjectUpdate"], function (ProjectUpdate) {
                        var projectUpdate = new ProjectUpdate();
                        projectUpdate.setProjectId(moveEntityModel.entity.getIdentifier());
                        projectUpdate.setSpaceId(moveEntityModel.selected.getPermId());
                        mainController.openbisV3.updateProjects([projectUpdate]).done(done).fail(fail);
                    });
                    break;
            }
		}
		if(moveEntityModel.isNewExperiment) {
            var experimentType = moveEntityModel.experimentType;
            var experimentIdentifier = moveEntityModel.experimentIdentifier;
            var projectIdentifier = IdentifierUtil.getProjectIdentifierFromExperimentIdentifier(experimentIdentifier);
            var code = IdentifierUtil.getCodeFromIdentifier(experimentIdentifier)
            mainController.serverFacade.createExperiment(experimentType, projectIdentifier, code, function(result) {
                var experimentPermId = result[0].permId;
                var criteria = { entityKind : "EXPERIMENT", logicalOperator : "AND", rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : experimentPermId } } };
                mainController.serverFacade.searchForExperimentsAdvanced(criteria, { only: true,
                                                                                       withProject: true,
                                                                                       withProjectSpace: true }, function(experimentSearchResult) {
                    var newExperiment = experimentSearchResult.objects[0];
                    mainController.sideMenu.refreshNodeByPermId("PROJECT", newExperiment.getProject().getPermId().getPermId());
                    moveEntityModel.selected = newExperiment;
                    moveEntityFunction();
                });
            });
        } else {
            moveEntityFunction();
        }
		
	}
	
    this.gatherAllDescendants = function(entities, entity) {
        entities.push(entity);
        entity.getChildren().forEach(child => this.gatherAllDescendants(entities, child));
    }
}