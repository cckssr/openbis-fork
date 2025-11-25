/*
 * Copyright 2014 - 2024 ETH Zuerich, Scientific IT Services
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

function DataSetFormView(dataSetFormController, dataSetFormModel) {
	this._dataSetFormController = dataSetFormController;
	this._dataSetFormModel = dataSetFormModel;
	this._previousGlobalEventListener = null;
    this._dataSetFormViewGlobalEventListener = null;
    this._viewId = mainController.getNextId();
	this.extOpenbis = window.NgComponents.default.openbis
	this.extOpenbis.init(mainController.openbisV3)

    var _refreshableFields = [];

    this.refresh = function() {
        if(this._dataSetFormModel.mode === FormMode.CREATE) {
            var $datasetDropdown = $("#DATASET_TYPE");
            if($datasetDropdown.length > 0) {
                Select2Manager.add($datasetDropdown);
            }
        }

        if(this._dataSetFormModel.datasetParentsComponent) {
            this._dataSetFormModel.datasetParentsComponent.initSelect2()
        }

        var $metadataContainer = $("#metadataContainer-"+this._viewId+" select:not([id^=sample-field-id])");
        if($metadataContainer.length > 0) {
            Select2Manager.add($metadataContainer);
        }


        for(var field of _refreshableFields) {
            field.refresh();
        }

    }

	this.repaint = function(views) {
		var $container = views.content;
        mainController.profile.beforeViewPaint(ViewType.DATASET_FORM, this._dataSetFormModel, $container);
		var _this = this;
        var dataSetTypeDefinitionsExtension = profile.dataSetTypeDefinitionsExtension[this._getTypeCode()];

		//Clean and prepare container
		var $wrapper = $('<form>', { 'id' : 'mainDataSetForm-'+_this._viewId, 'role' : 'form'});
		if(this._dataSetFormModel.isMini) {
			$wrapper.css('margin', '10px');
			$wrapper.css('padding', '10px');
			$wrapper.css('background-color', '#f8f8f8');
		}
		$wrapper.submit(function(event) {_this._dataSetFormController.submitDataSet(); event.preventDefault();});
		hideShowOptionsModel = [];
        $wrapper.append(this._createIdentificationInfoSection(hideShowOptionsModel, views));

        const shouldUseAFSWidget = this._dataSetFormModel.mode === FormMode.VIEW && !this._dataSetFormModel.isMini && profile.isAFSAvailable();
        const $formContainer = $('<div>', { class : 'row'}).append($('<div>', { class : FormUtil.formColumClass}).append($wrapper));

        this._repaintHeader(views, shouldUseAFSWidget ? $formContainer : null, shouldUseAFSWidget ? $container : null);

		// Plugin Hook
		if (!this._dataSetFormModel.isMini) {
			var $datasetFormTop = new $('<div>');
			$wrapper.append($datasetFormTop);
			profile.dataSetFormTop($datasetFormTop, this._dataSetFormModel);
		}
		
		//Metadata Container
		$wrapper.append($('<div>', { 'id' : 'metadataContainer-'+_this._viewId}));
		
		//Attach File
		$wrapper.append($('<div>', { 'id' : 'APIUploader-'+_this._viewId } ));
		
		$wrapper.append($('<div>', { 'id' : 'fileOptionsContainer-'+_this._viewId } ));
		
		// Plugin Hook
		if(!this._dataSetFormModel.isMini) {
			var $datasetFormBottom = new $('<div>');
			$wrapper.append($datasetFormBottom);
			profile.dataSetFormBottom($datasetFormBottom, this._dataSetFormModel);
		}
		
		
		//Show Files
		var filesViewer = $('<div>', { 'id' : 'filesViewer-'+_this._viewId } );
		$wrapper.append(filesViewer);
		
		//Submit Button
		if(this._dataSetFormModel.mode !== FormMode.VIEW) {
			if(_this._dataSetFormModel.isMini) {
				var btnText = "";
				if(this._dataSetFormModel.mode === FormMode.CREATE) {
					btnText = 'Create';
				} else if(this._dataSetFormModel.mode === FormMode.EDIT) {
					btnText = 'Update';
				}
				
				var $submitButton = $('<fieldset>')
				.append($('<div>', { class : "form-group"}))
				.append($('<div>')
							.append($('<input>', { class : 'btn btn-primary', 'type' : 'submit', 'value' : btnText})));
				
				$wrapper.append($submitButton);
				
				var $autoUploadCheck = FormUtil._getBooleanField(null, 'Auto upload on drop');
					$($autoUploadCheck.children()[0]).children()[0].checked = _this._dataSetFormModel.isAutoUpload;
				
					$autoUploadCheck.css("display","inline");
					$autoUploadCheck.css("padding-top", "2px");
					$autoUploadCheck.change(function(){
						var isChecked = $($(this).children()[0]).children()[0].checked;
						_this._dataSetFormModel.isAutoUpload = isChecked;
						mainController.serverFacade.setSetting("DataSetFormModel.isAutoUpload", isChecked);
					});
					
				var $autoUploadGroup = $('<fieldset>')
						.append($('<div>', { class : "form-group"}))
						.append($('<div>')
						.append($autoUploadCheck).append(" Auto upload on drop"));
				
				$wrapper.append($('<fieldset>').append($autoUploadGroup));
			}
		}
		
		//Attach to main form
			if(!shouldUseAFSWidget) {
				$container.append($formContainer);
			}
		
		if(this._dataSetFormModel.mode === FormMode.CREATE) {
			//Initialize file chooser
			var onComplete = function(data) {
				_this._dataSetFormModel.files.push(data.name);
				if(!_this._dataSetFormModel.isMini){
					_this._updateFileOptions();
				}
				var dataSetTypeCode = profile.getDataSetTypeForFileName(_this._dataSetFormModel.files, data.name);
				if(dataSetTypeCode != null) {
					var selectedDataSetTypeCode = $("#DATASET_TYPE").val();
					if(selectedDataSetTypeCode !== dataSetTypeCode) {
						$("#DATASET_TYPE").val(dataSetTypeCode);
						if(!_this._dataSetFormModel.isMini){
							_this._repaintMetadata(
									_this._dataSetFormController._getDataSetType(dataSetTypeCode)
							);
						}
					}
				}
				
				if(_this._dataSetFormModel.isMini && !Uploader.uploadsInProgress() && _this._dataSetFormModel.isAutoUpload) {
					if($("#DATASET_TYPE").val()) {
						_this._dataSetFormController.submitDataSet();
					} else {
						var showSelectDatasetType = function() {
                            var dataSetTypes = profile.filterDataSetTypesForDropdowns(_this._dataSetFormModel.dataSetTypes);
                            var $dropdown = FormUtil.getDataSetsDropDown("datasetTypeForDataset", dataSetTypes);
							Util.showDropdownAndBlockUI("datasetTypeForDataset", $dropdown);
							
							$("#datasetTypeForDataset").on("change", function(event) {
								var datasetTypeCode = $("#datasetTypeForDataset")[0].value;
								$("#DATASET_TYPE").val(datasetTypeCode);
								_this._dataSetFormController.submitDataSet();
							});
							
							$("#datasetTypeForDatasetCancel").on("click", function(event) { 
								Util.unblockUI();
							});
						}
						showSelectDatasetType();
					}
					
				}
			}
			
			var onDelete = function(data) {
				for(var i=0; _this._dataSetFormModel.files.length; i++) {
					if(_this._dataSetFormModel.files[i] === data.name) {
						_this._dataSetFormModel.files.splice(i, 1);
						break;
					}
				}
				if(!_this._dataSetFormModel.isMini){
					_this._updateFileOptions();
				}
			}
			
			if(this._dataSetFormModel.mode === FormMode.CREATE) {
				mainController.serverFacade.openbisServer.createSessionWorkspaceUploader($("#APIUploader-"+_this._viewId), onComplete, {
					main_title : $('<legend>').text('Files Uploader'),
					uploads_title : $('<legend>').text('File list'),
					ondelete:onDelete,
					hideHint:_this._dataSetFormModel.isMini,
					viewId: _this._viewId,
				});
			}
		} else {
			var dataSetType = _this._dataSetFormController._getDataSetType(this._getTypeCode());
			this._repaintMetadata(dataSetType);
		}
		
		if(this._dataSetFormModel.mode !== FormMode.CREATE) {
			var dataSetViewer = new DataSetViewerController("filesViewer-"+_this._viewId, profile, this._dataSetFormModel.entity, mainController.serverFacade, profile.getDefaultDataStoreURL(), [this._dataSetFormModel.dataSetV3], false, true);
			dataSetViewer.init();
		}
        mainController.profile.afterViewPaint(ViewType.DATASET_FORM, this._dataSetFormModel, $container);
	}
	
	this._addArchivingButton = function(toolbarModel, toolbarConfig) {
		var _this = this;

		var physicalData = this._dataSetFormModel.dataSetV3.physicalData;

		if (profile.showDatasetArchivingButton && physicalData) {


            var archiveIcon = "ARCHIVE_NOT_REQUESTED";
			var archiveImage = "./img/archive-not-requested-icon.png";
			if (physicalData.presentInArchive) {
			    archiveIcon = "ARCHIVED";
				archiveImage = "./img/archive-archived-icon.png";
			} else if (physicalData.archivingRequested || physicalData.status == "ARCHIVE_PENDING") {
				archiveImage = "./img/archive-requested-icon.png";
				archiveIcon = "ARCHIVE_REQUESTED";
			}

			var archiveAction = null;
			var archiveTooltip = null;
			var $archiveTooltip = null;
			if (physicalData.status == "AVAILABLE" && !physicalData.presentInArchive) {
				if (physicalData.archivingRequested) {
					archiveAction = function() {
						_this.revokeArchivingRequest();
					}
					archiveTooltip = "Revoke archiving request";
				} else {
					archiveAction = function() {
						_this.requestOrLockArchiving();
					}
					archiveTooltip = "Request or disallow archiving";
				}
			} else if (physicalData.status == "LOCKED") {
				archiveAction = function() {
					_this._dataSetFormController.setArchivingLock(false);
				}
				archiveTooltip = "Allow archiving";
			} else if (physicalData.status == "ARCHIVED") {
				archiveAction = function() {
					_this._dataSetFormController.unarchive();
				}
				archiveTooltip = "Unarchive";
			}

			if (archiveTooltip == null) {
				$archiveTooltip = $("<div>")
					.append($("<p>").text("Status: " + physicalData.status))
					.append($("<p>").text("Present in archive: " + physicalData.presentInArchive))
					.append($("<p>").text("Archiving requested: " + physicalData.archivingRequested));
			}

            var $archivingRequestedBtn = FormUtil.getToolbarButton(archiveIcon, archiveAction, archiveTooltip, archiveTooltip, "dataset-archiving-btn-", 'btn btn-primary btn-secondary');
			if (archiveAction == null) {
				$archivingRequestedBtn.attr("disabled", true);
			}

			if(toolbarConfig.ARCHIVE) {
				toolbarModel.push({
					component : $archivingRequestedBtn,
					tooltip : archiveTooltip,
					$tooltip : $archiveTooltip
				});
			}
		}
		
	}

	this._repaintHeader = function(views, $form, $container) {
		$form = $form || null;
		$container = $container || null;
	    var _this = this;
		//
		// Title
		//
		var titleText = null;
		if(this._dataSetFormModel.mode === FormMode.CREATE) {
			titleText = 'Create Dataset';
		} else {
		    var nameLabel = FormUtil.getDataSetName(this._dataSetFormModel.dataSetV3.code, this._dataSetFormModel.dataSetV3.properties)
            if(this._dataSetFormModel.mode === FormMode.EDIT) {
                titleText = 'Update Dataset: ' + nameLabel;
            } else if(this._dataSetFormModel.mode === FormMode.VIEW) {
                titleText = 'Dataset: ' + nameLabel;
            }
		}
		var $title = $('<div>');
		$title.append($("<h2>").append(titleText));

		//
		// Toolbar
		//
			var toolbarModel = [];
			var rightToolbarModel = [];
			var altRightToolbarModel = [];
			var shouldUseTabs = $form && $container && this._dataSetFormModel.mode === FormMode.VIEW && !this._dataSetFormModel.isMini && profile.isAFSAvailable();
		var dropdownOptionsModel = [];
		if(this._dataSetFormModel.mode === FormMode.VIEW && !this._dataSetFormModel.isMini) {
			var toolbarConfig = profile.getDataSetTypeToolbarConfiguration(this._getTypeCode());
			if (_this._allowedToEdit()) {
				//Edit Button
				var $editBtn = FormUtil.getToolbarButton("EDIT", function () {
				    Util.blockUI();
				    var arg = {
                            permIdOrIdentifier : _this._dataSetFormModel.dataSetV3.code,
                            paginationInfo : _this._dataSetFormModel.paginationInfo
                    }
					mainController.changeView('showEditDataSetPageFromPermId', arg);
				}, "Edit", "Edit data", "dataset-edit-btn-"+_this._viewId, 'btn btn-primary btn-secondary');
				if(toolbarConfig.EDIT) {
					toolbarModel.push({ component : $editBtn });
				}
			}

			this._addArchivingButton(toolbarModel, toolbarConfig);

			if(_this._allowedToMove()) {
				//Move
				if(toolbarConfig.MOVE) {
                    dropdownOptionsModel.push({
                        label : "Move",
                        action : function() {
                                var moveEntityController = new MoveEntityController("DATASET", _this._dataSetFormModel.dataSetV3.code);
                                moveEntityController.init();
                        }
                    });
				}
			}
			if(_this._allowedToDelete()) {
				//Delete Button
				if(toolbarConfig.DELETE) {
                    dropdownOptionsModel.push({
                        label : "Delete",
                        action : function() {
                            var modalView = new DeleteEntityController(function(reason) {
					            _this._dataSetFormController.deleteDataSet(reason);
					            mainController.tabContent.closeCurrentTab();
                            }, true);
                            modalView.init();
                        }
                    });
				}
			}

            //Print
            dropdownOptionsModel.push(FormUtil.getPrintPDFButtonModel("DATASET",  _this._dataSetFormModel.dataSetV3.code));

			//Hierarchy Table
			if(toolbarConfig.HIERARCHY_TABLE) {
				dropdownOptionsModel.push({
                    label : "Hierarchy Table",
                    action : function() {
                        mainController.changeView('showDatasetHierarchyTablePage', _this._dataSetFormModel.dataSetV3.code);
                    }
                });
			}

			//Export
			dropdownOptionsModel.push(FormUtil.getExportButtonModel("DATASET", _this._dataSetFormModel.dataSetV3.code));

            if (this._dataSetFormModel.availableProcessingServices.length > 0) {
                dropdownOptionsModel.push({
                    label : "Process",
                    action : function() {
                        var $window = $('<form>', {
                            'action' : 'javascript:void(0);'
                        });

                        $window.append($('<legend>').append("Process Data Set"));
                        var services = [];
                        var servicesById = {};
                        _this._dataSetFormModel.availableProcessingServices.forEach(function(service) {
                            var id = service.getPermId().toString();
                            services.push({value:id, label:service.getLabel()});
                            servicesById[id] = service;
                        });
                        var $serviceDropdown = FormUtil.getDropdown(services, "Select a processing service");
                        $window.append($serviceDropdown);
                        var $btnAccept = $('<input>', { 'type': 'submit', 'class' : 'btn btn-primary', 'value' : 'Accept' });
                        var $btnCancel = $('<a>', { 'class' : 'btn btn-default' }).append('Cancel');
                        $btnCancel.click(function() {
                            Util.unblockUI();
                        });
                        $window.append($('<br>'));
                        $window.append($btnAccept).append('&nbsp;').append($btnCancel);
                        $window.submit(function() {
                            Util.blockUI();
                            var service = servicesById[$serviceDropdown.val()];
                            var dataSetCode = _this._dataSetFormModel.dataSetV3.getCode();
                            mainController.serverFacade.processDataSets(service.getPermId(), [dataSetCode], function() {
                                Util.unblockUI();
                                Util.showInfo("Processing task '" + service.getLabel()
                                        + "' successfully submitted for data set " + dataSetCode + ".");
                            });
                        });
                        var css = {
                                'text-align' : 'left',
                                'top' : '15%',
                                'width' : '50%',
                                'left' : '15%',
                                'right' : '20%',
                                'overflow' : 'auto'
                        };
                        Util.blockUI($window, css);
                    }
                });
            }

			//Jupyter Button
			if(profile.jupyterIntegrationServerEndpoint) {
                dropdownOptionsModel.push({
                    label : "New Jupyter notebook",
                    action : function () {
                        var jupyterNotebook = new JupyterNotebookController(_this._dataSetFormModel.dataSetV3);
                        jupyterNotebook.init();
                    }
                });
			}

            //Freeze
            if(_this._dataSetFormModel.v3_dataset && _this._dataSetFormModel.v3_dataset.frozen !== undefined) { //Freezing available on the API
                var isEntityFrozen = _this._dataSetFormModel.v3_dataset.frozen;
                if(toolbarConfig.FREEZE) {
                    if(isEntityFrozen) {
                        var $freezeButton = FormUtil.getFreezeButton("DATASET", this._dataSetFormModel.v3_dataset.permId.permId, isEntityFrozen);
                        toolbarModel.push({ component : $freezeButton, tooltip: "Entity Frozen" });
                    } else {
                        dropdownOptionsModel.push({
                            label : "Freeze Entity (Disable further modifications)",
                            action : function () {
                                FormUtil.showFreezeForm("DATASET", _this._dataSetFormModel.v3_dataset.permId.permId, _this._dataSetFormModel.v3_dataset.code);
                            }
                        });
                    }
                }
            }

            //History
            if(toolbarConfig.HISTORY) {
                dropdownOptionsModel.push({
                    label : "History",
                    action : function() {
                        mainController.changeView('showDatasetHistoryPage', _this._dataSetFormModel.dataSetV3.code);
                    }
                });
            }
		} else if(!this._dataSetFormModel.isMini) {
			var $saveBtn = FormUtil.getToolbarButton("SAVE", function() {
				_this._dataSetFormController.submitDataSet();
				if(!_this._wasSideMenuCollapsed) {
                    mainController.sideMenu.expandSideMenu();
                }
			}, "Save", "Save changes", "save-dataset-btn-"+this._viewId);
			$saveBtn.removeClass("btn-default");
			$saveBtn.addClass("btn-primary");
			toolbarModel.push({ component : $saveBtn });
		}

		if (!this._dataSetFormModel.isMini) {
		    views.header.empty();
			var $header = views.header;
			$header.append($title);

			// Toolbar extension
			var datasetTypeCode = null;
			if(this._dataSetFormModel.mode === FormMode.CREATE) {
			    datasetTypeCode = $("#DATASET_TYPE").val();
			}
			if(this._dataSetFormModel.mode === FormMode.EDIT || this._dataSetFormModel.mode === FormMode.VIEW) {
			    datasetTypeCode = this._getTypeCode();
			}
			if(datasetTypeCode) {
                if(profile.dataSetTypeDefinitionsExtension[datasetTypeCode] && profile.dataSetTypeDefinitionsExtension[datasetTypeCode].extraToolbar) {
                    toolbarModel = toolbarModel.concat(profile.dataSetTypeDefinitionsExtension[datasetTypeCode].extraToolbar(_this._dataSetFormModel.mode, _this._dataSetFormModel.dataSetV3));
                }
                if(profile.dataSetTypeDefinitionsExtension[datasetTypeCode] && profile.dataSetTypeDefinitionsExtension[datasetTypeCode].extraToolbarDropdown) {
                    dropdownOptionsModel = dropdownOptionsModel.concat(profile.dataSetTypeDefinitionsExtension[datasetTypeCode].extraToolbarDropdown(_this._dataSetFormModel.mode, _this._dataSetFormModel.dataSetV3));
                }
            }

			FormUtil.addOptionsToToolbar(toolbarModel, dropdownOptionsModel, hideShowOptionsModel, "DATA-SET-VIEW");
			var $helpBtn = FormUtil.getToolbarButton("?", function() {
                mainController.openHelpPage();
            }, null, "Documentation", "help-dataset-btn-"+this._viewId, 'btn btn-default help');
            $helpBtn.find("span").css("vertical-align", "middle").css("font-size", "24px")
            toolbarModel.push({ component : $helpBtn });
			if(!shouldUseTabs) {
				$header.append(FormUtil.getToolbar(toolbarModel));
			}
		}

		if(this._dataSetFormModel.mode === FormMode.VIEW && this._dataSetFormModel.paginationInfo && this._dataSetFormModel.paginationInfo.pagFunction) {
            var moveToIndex = function(index) {
                if(index < 0 || _this._dataSetFormModel.paginationInfo.totalCount == index) {
                    return;
                }
                var pagOptionsToSend = $.extend(true, {}, _this._dataSetFormModel.paginationInfo.pagOptions);
                pagOptionsToSend.pageIndex = index;
                pagOptionsToSend.pageSize = 1;
                _this._dataSetFormModel.paginationInfo.pagFunction(function(result) {
                    if(result && result[0] && result[0].permId) {
                        var paginationInfo = $.extend(true, {}, _this._dataSetFormModel.paginationInfo);
                        paginationInfo.previousIndex = _this._dataSetFormModel.paginationInfo.currentIndex;
                        paginationInfo.currentIndex = index;
                        var arg = {
                                permIdOrIdentifier : result[0].permId.permId,
                                paginationInfo : paginationInfo
                        }
                        mainController.changeView('showViewDataSetPageFromPermId', arg, true);
                        mainController.tabContent.replaceTabs(_this._dataSetFormModel.dataSetV3.code, result[0].permId.permId)
                        var objectId = { type: "DATASET", id: result[0].permId.permId };
                        if(mainController.sideMenu.hasNodeWithObjectId(objectId)) {
                            mainController.sideMenu.moveToNodeIdAfterLoad(objectId);
                        }
                    } else {
                        window.alert("The item to go to is no longer available.");
                    }
                }, pagOptionsToSend);
            }

            var arrowKeyEventListener = function(paginationInfo) {
                return function(event) {
                        if(event.key === "ArrowRight") {
                            if(paginationInfo.currentIndex+1 < paginationInfo.totalCount) {
                                moveToIndex(paginationInfo.currentIndex+1);
                            }
                        } else if(event.key === "ArrowLeft") {
                            if(paginationInfo.currentIndex-1 >= 0) {
                               moveToIndex(paginationInfo.currentIndex-1);
                            }
                        } else {
                            // Ignore others
                        }
                };
            }

            if(this._dataSetFormModel.paginationInfo) {

                mainController.currentView.finalize = function(backButtonLogic) {
                    if(!backButtonLogic) {
                       _this._previousGlobalEventListener =  _this._dataSetFormViewGlobalEventListener;
                    }
                    document.removeEventListener('keyup', _this._dataSetFormViewGlobalEventListener);
                }

                this._dataSetFormViewGlobalEventListener = arrowKeyEventListener(_this._dataSetFormModel.paginationInfo);
                document.addEventListener('keyup',  this._dataSetFormViewGlobalEventListener);

                var $backBtn = FormUtil.getToolbarButton("PAGINATION_LEFT", function () {
                                    moveToIndex(_this._dataSetFormModel.paginationInfo.currentIndex-1);
                }, null, null, "back-btn-dataset-"+_this._viewId);

                if(this._dataSetFormModel.paginationInfo.currentIndex <= 0) {
                    $backBtn.attr("disabled",true);
                    $backBtn.off('click');
                }

                var $paginationInfoLabel = this._dataSetFormModel.paginationInfo.currentIndex+1 + " of " + this._dataSetFormModel.paginationInfo.totalCount;
                    $paginationInfoLabel = $("<span>").text($paginationInfoLabel).css('margin-right', '8px');

                var $nextBtn = FormUtil.getToolbarButton("PAGINATION_RIGHT", function () {
                                        moveToIndex(_this._dataSetFormModel.paginationInfo.currentIndex+1);
                }, null, null, "forward-btn-dataset-"+_this._viewId);

                if(this._dataSetFormModel.paginationInfo.currentIndex+1 >= this._dataSetFormModel.paginationInfo.totalCount) {
                    $nextBtn.attr("disabled",true);
                    $nextBtn.off('click');
                }

                rightToolbarModel.push({ component : $backBtn, tooltip: null });
                rightToolbarModel.push({ component : $paginationInfoLabel, tooltip: null });
                rightToolbarModel.push({ component : $nextBtn, tooltip: null });

				altRightToolbarModel.push({ component : $backBtn.clone(true), tooltip: null });
				altRightToolbarModel.push({ component : $paginationInfoLabel.clone(true), tooltip: null });
                altRightToolbarModel.push({ component : $nextBtn.clone(true), tooltip: null });
            }
            if(shouldUseTabs) {
					this._initTabHandling($header, $container, $form, toolbarModel, rightToolbarModel, altRightToolbarModel.length > 0 ? altRightToolbarModel : rightToolbarModel);
			} else {
            	$header.append(FormUtil.getToolbar(rightToolbarModel).css("float", "right"));
			}
        }



	}

	this._createIdentificationInfoSection = function(hideShowOptionsModel, views) {
		hideShowOptionsModel.push({
			forceToShow : this._dataSetFormModel.mode === FormMode.CREATE,
			label : "Identification Info",
			section : "#data-set-identification-info-"+this._viewId
		});
		
		var _this = this;

		var $dataSetTypeFieldSet = $('<div>', { id : "data-set-identification-info-"+_this._viewId });
		if (!this._dataSetFormModel.isMini) {
			$dataSetTypeFieldSet.append($('<legend>').text('Identification Info'));
			if (this._dataSetFormModel.mode !== FormMode.CREATE) {
                $dataSetTypeFieldSet.append(FormUtil.getFieldForLabelWithText("PermId", this._dataSetFormModel.dataSetV3.code));
            }
		}
		var $dataSetTypeSelector = null;
		if (this._dataSetFormModel.mode === FormMode.CREATE) {
            var dataSetTypes = profile.filterDataSetTypesForDropdowns(_this._dataSetFormModel.dataSetTypes);
            $dataSetTypeSelector = FormUtil.getDataSetsDropDown('DATASET_TYPE', dataSetTypes);
            $dataSetTypeSelector.refresh = function() {
                Select2Manager.add(this);
            }
            _refreshableFields.push($dataSetTypeSelector)
			$("body").on("change", "#DATASET_TYPE", function() {
				if (!_this._dataSetFormModel.isMini) {
					_this._repaintMetadata(
							_this._dataSetFormController._getDataSetType($('#DATASET_TYPE').val())
					);
					_this._repaintHeader(views);
				}
				_this.isFormDirty = true;
			});
			var $dataSetTypeDropDown = $('<div>', { class : 'form-group' });
			if (!this._dataSetFormModel.isMini) {
				$dataSetTypeDropDown.append($('<label>', {class: "control-label"}).html('Data Set Type&nbsp;(*):'));
			}
			
			var $dataSetTypeDropDowContainer = $('<div>');
			if (this._dataSetFormModel.isMini) {
				$dataSetTypeDropDowContainer.css('width', '100%');
			}
			$dataSetTypeDropDown.append(
				$dataSetTypeDropDowContainer.append($dataSetTypeSelector)
			);
			$dataSetTypeFieldSet.append($dataSetTypeDropDown);
		} else {
			$dataSetTypeFieldSet.append(FormUtil.getFieldForComponentWithLabel(this._createEntityPath(), "Path"));
			var $dataSetTypeLabel = FormUtil.getFieldForLabelWithText('Data Set Type', this._getTypeCode(), "CODE");
			$dataSetTypeFieldSet.append($dataSetTypeLabel);
			var $dataSetCodeLabel = FormUtil.getFieldForLabelWithText('Code', this._dataSetFormModel.dataSetV3.code, null);
			$dataSetTypeFieldSet.append($dataSetCodeLabel);
		}
		
		// Parents
		var $dataSetParentsCodeLabel = $("<div>");
		if (this._dataSetFormModel.mode === FormMode.VIEW) {
			for(var i = 0; i < this._dataSetFormModel.dataSetV3.parents.length; i++) {
					var parent = this._dataSetFormModel.dataSetV3.parents[i];
					var $span = $("<span>");
					$span.append(FormUtil.getFormLink(parent.permId.permId, "DataSet", parent.permId.permId));
					var name = parent.properties[profile.propertyReplacingCode];
					if(name) {
						$span.append(" : ").append(name);
					}
					$dataSetParentsCodeLabel.append($("<div>").append($span));
			}
			
		} else if (!this._dataSetFormModel.isMini) {
                var dataSetType = this._getTypeCode();
                var parentsEditingDisabled = profile.dataSetTypeDefinitionsExtension[dataSetType] && profile.dataSetTypeDefinitionsExtension[dataSetType]["DATASET_PARENTS_DISABLED"]
			if (!parentsEditingDisabled) {
				this._dataSetFormModel.datasetParentsComponent = new AdvancedEntitySearchDropdown(true, false, "Search parents to add",
						false, false, true, false, false);
				this._dataSetFormModel.datasetParentsComponent.init($dataSetParentsCodeLabel);
				var parentCodes;
				if (this._dataSetFormModel.dataSetV3)
				{
    				parentCodes = this._dataSetFormModel.dataSetV3.parents.map(dataset => dataset.permId.permId);
				}
				this._dataSetFormModel.datasetParentsComponent.addSelectedDataSets(parentCodes);
			}
		}
		
		if ((this._dataSetFormModel.mode === FormMode.VIEW && this._dataSetFormModel.dataSetV3.parents.length !== 0) 
			|| 
			(this._dataSetFormModel.mode !== FormMode.VIEW && !this._dataSetFormModel.isMini)
			) {
			$dataSetTypeFieldSet.append(FormUtil.getFieldForComponentWithLabel($dataSetParentsCodeLabel, 'Parents'));
		}
		
		// Children
		
		var $dataSetChildrenCodeLabel = $("<div>");
		if (this._dataSetFormModel.mode === FormMode.VIEW) {
			for (var i = 0; i < this._dataSetFormModel.dataSetV3.children.length; i++) {
					var child = this._dataSetFormModel.dataSetV3.children[i];
					var $span = $("<span>");
					$span.append(FormUtil.getFormLink(child.permId.permId, "DataSet", child.permId.permId));
					var name = child.properties[profile.propertyReplacingCode];
					if(name) {
						$span.append(" : ").append(name);
					}
					$dataSetChildrenCodeLabel.append($("<div>").append($span));
			}
		}
		
		if (this._dataSetFormModel.mode === FormMode.VIEW && this._dataSetFormModel.dataSetV3.children.length !== 0) {
			$dataSetTypeFieldSet.append(FormUtil.getFieldForComponentWithLabel($dataSetChildrenCodeLabel, 'Children'));
		}
		
		//
		
		var ownerName = null;
		var owner = null;
		if (this._dataSetFormModel.isExperiment()) { 
			ownerName = ELNDictionary.ExperimentELN; //Only experiments on the ELN have datasets
			owner = this._dataSetFormModel.entity.identifier.identifier;
		} else {
			ownerName = ELNDictionary.Sample;
			owner = this._dataSetFormModel.entity.identifier;
		}
		
		if (!this._dataSetFormModel.isMini) {
			$dataSetTypeFieldSet.append(FormUtil.getFieldForLabelWithText(ownerName, owner));
		}
		
		//
		// Content copies info
		//
		if (this._dataSetFormModel.linkedData && this._dataSetFormModel.linkedData.contentCopies) {
			var $ccn = FormUtil.getFieldForLabelWithText("Number of content copies", "" + this._dataSetFormModel.linkedData.contentCopies.length);
			$dataSetTypeFieldSet.append($ccn);
			for (var cIdx = 0; cIdx < this._dataSetFormModel.linkedData.contentCopies.length; cIdx++) {
				var cc = this._dataSetFormModel.linkedData.contentCopies[cIdx];
				
				var externalDmsCode = null;
				if (cc.externalDms && cc.externalDms.code) {
					externalDmsCode = cc.externalDms.code;
				}
				
				var host = null;
				if (cc.externalDms && cc.externalDms.address) {
					host = cc.externalDms.address.split(":")[0];
				}
				
				if (cc) {
					var $cc = FormUtil.getFieldForLabelWithText("Content Copy " + (cIdx+1) , 
							"- <u>External DMS</u>: " + externalDmsCode + "<br>" + 
							"- <u>Host</u>: " + host + "<br>" +
							"- <u>Directory</u>: " + cc.path + "<br>" +
							"- <u>Commit Hash</u>: " + cc.gitCommitHash + "<br>" + 
							"- <u>Repository Id</u>: " + cc.gitRepositoryId + "<br>" +
							"- <u>Connect cmd</u>: " +  "ssh -t " + host + " \"cd " + cc.path + "; bash\""
							);
					$dataSetTypeFieldSet.append($cc);
				}
			}
			
		}
		
		//
		// Registration and modification info
		//
		if (this._dataSetFormModel.mode !== FormMode.CREATE) {
			var $registrator = FormUtil.getFieldForLabelWithText("Registrator", (_this._dataSetFormModel.dataSetV3.registrator)?_this._dataSetFormModel.dataSetV3.registrator.userId:'');
			$dataSetTypeFieldSet.append($registrator);
			
			var $registationDate = FormUtil.getFieldForLabelWithText("Registration Date", Util.getFormatedDate(new Date(_this._dataSetFormModel.dataSetV3.registrationDate)))
			$dataSetTypeFieldSet.append($registationDate);

			var $modifier = FormUtil.getFieldForLabelWithText("Modifier", (_this._dataSetFormModel.dataSetV3.modifier.userId)?_this._dataSetFormModel.dataSetV3.modifier.userId:'');
			$dataSetTypeFieldSet.append($modifier);
			
			var $modificationDate = FormUtil.getFieldForLabelWithText("Modification Date", Util.getFormatedDate(new Date(_this._dataSetFormModel.dataSetV3.modificationDate)));
			$dataSetTypeFieldSet.append($modificationDate);
		}
		$dataSetTypeFieldSet.hide();
		return $dataSetTypeFieldSet;
	}
	
	this._createEntityPath = function() {
		var spaceCode;
		var projectCode;
		var experimentCode;
		
		var experimentIdentifier = null;
		if (this._dataSetFormModel.isExperiment()) {
			experimentIdentifier = this._dataSetFormModel.entity.identifier.identifier;
		} else { //Both Sample and Experiment exist
			experimentIdentifier = this._dataSetFormModel.entity.experimentIdentifierOrNull;
		}
		if (experimentIdentifier) {
			spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(experimentIdentifier);
			projectCode = IdentifierUtil.getProjectCodeFromExperimentIdentifier(experimentIdentifier);
			experimentCode = IdentifierUtil.getCodeFromIdentifier(experimentIdentifier);
		}
		var sampleCode;
		var sampleIdentifier;
		if (!this._dataSetFormModel.isExperiment()) {
			sampleCode = this._dataSetFormModel.entity.code;
			spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(this._dataSetFormModel.entity.identifier);
			sampleIdentifier = this._dataSetFormModel.entity.identifier;
		}
		var datasetCodeAndPermId = this._getTypeCode();
		return FormUtil.getFormPath(spaceCode, projectCode, experimentCode, null, null, sampleCode, 
				sampleIdentifier, datasetCodeAndPermId);
	}
	
	this._updateFileOptions = function() {
		var _this = this;
		$wrapper = $("#fileOptionsContainer-"+_this._viewId); //Clean existing
		$wrapper.empty();
		
		if( this._dataSetFormModel.files.length > 1
			||
			this._dataSetFormModel.files.length === 1 && this._dataSetFormModel.files[0].indexOf('zip', this._dataSetFormModel.files[0].length - 3) !== -1) {
			var $legend = $('<div>').append($('<legend>').text('Files Options'));
			$wrapper.append($legend);
		}
		
		if(this._dataSetFormModel.files.length > 1) {
			var $textField = FormUtil._getInputField('text', 'folderName', 'Folder Name', null, true);
			$textField.change(function(event) {
				_this.isFormDirty = true;
			});
			
			var $folderName = $('<div>')
			.append($('<div>', { class : "form-group"})
					.append($('<label>', {class : 'control-label'}).html('Folder Name&nbsp;(*):'))
					.append($('<div>')
						.append($textField))
			);
			$wrapper.append($folderName);
		}
		
		if(this._dataSetFormModel.files.length === 1 && 
				this._dataSetFormModel.files[0].indexOf('zip', this._dataSetFormModel.files[0].length - 3) !== -1) {
			var isZipDirectoryUpload = profile.isZipDirectoryUpload($('#DATASET_TYPE').val());
			if(isZipDirectoryUpload === null) {
				var $fileFieldSetIsDirectory = $('<div>')
				.append($('<div>', { class : "form-group"})
							.append($('<label>', {class : 'control-label'}).text('Uncompress before import:'))
							.append($('<div>')
								.append(FormUtil._getBooleanField('isZipDirectoryUpload', 'Uncompress before import:')))
				);
				$wrapper.append($fileFieldSetIsDirectory);
				
				$("#isZipDirectoryUpload").change(function() {
					_this.isFormDirty = true;
					if($("#isZipDirectoryUpload"+":checked").val() === "on") {
						var $textField = FormUtil._getInputField('text', 'folderName', 'Folder Name', null, true);
						$textField.change(function(event) {
							_this.isFormDirty = true;
						});
						
						var $folderName = $('<div>', { "id" : "folderNameContainer"})
						.append($('<div>', { class : "form-group"})
								.append($('<label>', {class : 'control-label' }).html('Folder Name&nbsp;(*):'))
								.append($('<div>')
									.append($textField))
						);
						$("#fileOptionsContainer-"+_this._viewId).append($folderName);
						$("#folderName").val(_this._dataSetFormModel.files[0].substring(0, _this._dataSetFormModel.files[0].indexOf(".")));
					} else {
						$( "#folderNameContainer" ).remove();
					}
				})
			}
		}
	}
	
	this._repaintMetadata = function(dataSetType) {
		var _this = this;
		$("#metadataContainer-"+_this._viewId).empty();
		if(dataSetType == null) {
		    return;
		}
		var $wrapper = $("<div>");
		var dataSetTypeV3 = null;
		for(var i = 0; i < _this._dataSetFormModel.dataSetTypesV3.length; i++) {
		    if(dataSetType.code === _this._dataSetFormModel.dataSetTypesV3[i].code) {
		        dataSetTypeV3 = _this._dataSetFormModel.dataSetTypesV3[i];
		        break;
		    }
		}
		for(var i = 0; i < dataSetType.propertyTypeGroups.length; i++) {
			var propertyTypeGroup = dataSetType.propertyTypeGroups[i];
			
			var $fieldset = $('<div>');
			var $legend = $('<legend>'); 
			$fieldset.append($legend);
			
			if(propertyTypeGroup.name) {
				$legend.text(propertyTypeGroup.name);
			} else if(dataSetType.propertyTypeGroups.length === 1) { //Only when there is only one group without name to render it with a default title.
				$legend.text("Metadata Fields");
			} else {
				$legend.remove();
			}
			
			var propertyGroupPropertiesOnForm = 0;
			for(var j = 0; j < propertyTypeGroup.propertyTypes.length; j++) {
				var propertyType = propertyTypeGroup.propertyTypes[j];
				profile.fixV1PropertyTypeVocabulary(propertyType);
				var propertyTypeV3 = profile.getPropertyTypeFromSampleTypeV3(dataSetTypeV3, propertyType.code);
				var isMultiValue = false;
                if(propertyTypeV3.isMultiValue) {
                    isMultiValue = propertyTypeV3.isMultiValue();
                }

				FormUtil.fixStringPropertiesForForm(propertyTypeV3, this._dataSetFormModel.dataSetV3);
				
				if(!propertyType.showInEditViews && (this._dataSetFormController.mode === FormMode.EDIT || this._dataSetFormController.mode === FormMode.CREATE) && propertyType.code !== "XMLCOMMENTS") { //Skip
					continue;
				} else if(propertyType.dinamic && this._dataSetFormController.mode === FormMode.CREATE) { //Skip
					continue;
				} else if(profile.isSystemProperty(propertyType)) {
				    continue;
				}
				
                if(propertyType.code === profile.getInternalNamespacePrefix() + "XMLCOMMENTS") {
					var $commentsContainer = $("<div>");
					$fieldset.append($commentsContainer);
					var isAvailable = this._dataSetFormController._addCommentsWidget($commentsContainer);
					if(!isAvailable) {
						continue;
					}
				} else {
					if(propertyType.code === profile.getInternalNamespacePrefix() + "SHOW_IN_PROJECT_OVERVIEW") {
						var identifier = null;
						if(this._dataSetFormModel.isExperiment()) { 
							identifier = this._dataSetFormModel.entity.identifier.identifier;
						} else {
							identifier = this._dataSetFormModel.entity.identifier;
						}
						
						if(!(profile.inventorySpaces.length > 0 && $.inArray(IdentifierUtil.getSpaceCodeFromIdentifier(identifier), profile.inventorySpaces) === -1)) {
							continue;
						}
					}
					
					var value = "";
					if(this._dataSetFormModel.mode !== FormMode.CREATE) {
						value = this._dataSetFormModel.dataSetV3.properties[propertyType.code];
						if(!value && propertyType.code.charAt(0) === '$') {
							value = this._dataSetFormModel.dataSetV3.properties[propertyType.code.substr(1)];
							this._dataSetFormModel.dataSetV3.properties[propertyType.code] = value;
							delete this._dataSetFormModel.dataSetV3.properties[propertyType.code.substr(1)];
						}
					}
					
					if(this._dataSetFormModel.mode === FormMode.VIEW) {
						if(Util.getEmptyIfNull(value) !== "") { //Don't show empty fields, whole empty sections will show the title
                            var customWidget = profile.customWidgetSettings[propertyType.code];
						    var forceDisableRTF = profile.isForcedDisableRTF(propertyType);
                            if(customWidget && !forceDisableRTF) {
                                if (customWidget === 'Spreadsheet') {
                                    var $jexcelContainer = $("<div>");
                                    JExcelEditorManager.createField($jexcelContainer, this._dataSetFormModel.mode, propertyType.code, this._dataSetFormModel.dataSetV3);
                                    $controlGroup = FormUtil.getFieldForComponentWithLabel($jexcelContainer, propertyType.label);
                                    $fieldset.append($controlGroup);
                                } else if (customWidget === 'Word Processor') {
                                    var $component = FormUtil.getFieldForPropertyType(propertyType, value);
                                    $component = FormUtil.activateRichTextProperties($component, undefined, propertyType, value, true);
                                    $controlGroup = FormUtil.getFieldForComponentWithLabel($component, propertyType.label);
                                    $fieldset.append($controlGroup);
                                }
                            } else if(propertyType.dataType === "SAMPLE") {
                                var $component = new SampleField(false, '', false, value, true, isMultiValue);
                                $controlGroup = FormUtil.getFieldForComponentWithLabel($component, propertyType.label);
                                $fieldset.append($controlGroup);
                            } else {
                                $controlGroup = FormUtil.createPropertyField(propertyType, value);
                                $fieldset.append($controlGroup);
                            }
                        } else {
                            continue;
                        }
					} else {
						var $controlGroup = $('<div>', {class : 'form-group'});
						var requiredStar = (propertyType.mandatory)?"&nbsp;(*)":"";				
						var $controlLabel = $('<label>', {'class' : "control-label" }).html(propertyType.label + requiredStar + ":");
						var $controls = $('<div>');
						
						$controlGroup.append($controlLabel);
						$controlGroup.append($controls);
						
						var $component = FormUtil.getFieldForPropertyType(propertyType, value, isMultiValue);
						if(['SAMPLE', 'DATE', 'TIMESTAMP'].includes(propertyType.dataType)) {
						    _refreshableFields.push($component);
						}

						//Update model
						var changeEvent = function(propertyType, isMultiValueProperty) {
							return function(jsEvent, newValue) {
								var propertyTypeCode = null;
								propertyTypeCode = propertyType.code;
								_this._dataSetFormModel.isFormDirty = true;
								var field = $(this);
								if(propertyType.dataType === "BOOLEAN") {
								    _this._setDataSetProperty(propertyTypeCode, Util.getEmptyIfNull(FormUtil.getBooleanValue(field)));
								} else if (propertyType.dataType === "TIMESTAMP" || propertyType.dataType === "DATE") {
									var timeValue = $($(field.children()[0]).children()[0]).val();
                                    var isValidValue = Util.isDateValid(timeValue, propertyType.dataType === "DATE");
                                    if(!isValidValue.isValid) {
                                        Util.showUserError(isValidValue.error);
                                    } else {
                                        _this._setDataSetProperty(propertyTypeCode, Util.getEmptyIfNull(timeValue));
                                    }
								} else {
									if(newValue !== undefined && newValue !== null) {
									    _this._setDataSetProperty(propertyTypeCode, Util.getEmptyIfNull(newValue));
									} else {
                                        var lastSelected = Util.getEmptyIfNull($('option', this).filter(':selected:last').val());
                                        var dataLast = field.data('last');
                                         if(propertyType.dataType === "CONTROLLEDVOCABULARY" && isMultiValueProperty) {
                                            var props = _this._getDataSetProperty(propertyTypeCode);
                                            if (field.val()) {
                                            if(props !== undefined) {
                                                if(props != '' && field.val().includes('')) {
                                                    _this._setDataSetProperty(propertyTypeCode, '');
                                                    field.val([]);
                                                } else {
                                                    if(props == '' && field.val().includes('')) {
                                                        var removedEmpty = field.val().filter(x => x != '');
                                                        _this._setDataSetProperty(propertyTypeCode, removedEmpty);
                                                        field.val(removedEmpty);
                                                    } else {
                                                        _this._setDataSetProperty(propertyTypeCode, Util.getEmptyIfNull(field.val()));
                                                    }
                                                }
                                            } else {
                                                if(field.val().includes('')) {
                                                    if(dataLast == undefined) {
                                                        var val = field.val().filter(x => x != '');
                                                        _this._setDataSetProperty(propertyTypeCode, val);
                                                        field.val(val);
                                                    } else {
                                                        _this._setDataSetProperty(propertyTypeCode, '');
                                                        field.val([]);
                                                    }
                                                } else {
                                                    _this._setDataSetProperty(propertyTypeCode, field.val());
                                                }
                                            }
                                             } else {
                                                _this._setDataSetProperty(propertyTypeCode, Util.getEmptyIfNull(field.val()));
                                             }
                                        } else {
                                            var value = Util.getEmptyIfNull(field.val());
                                            if(!profile.isOpenBIS6orNewer && Array.isArray(value)) {
                                                //there are no multi-value nor array properties in 20.10.x
                                                value = value[0];
                                            }
                                            _this._setDataSetProperty(propertyTypeCode, value);
                                        }
                                        field.data('last', field.val());
									}
								}
							}
						}
						
						//Update values if is into edit mode
						if(this._dataSetFormModel.mode === FormMode.EDIT) {
							if(propertyType.dataType === "BOOLEAN") {
							    FormUtil.setFieldValue(propertyType, $component, value);
							} else if(propertyType.dataType === "TIMESTAMP" || propertyType.dataType === "DATE") {
							} else if(isMultiValue) {
							    var valueV3 = this._dataSetFormModel.v3_dataset.properties[propertyType.code];
                                if(valueV3) {
                                    var valueArray;
                                    if(Array.isArray(valueV3)) {
                                        valueArray = valueV3.sort();
                                    } else {
                                        valueArray = valueV3.split(',');
                                        valueArray = valueArray.map(x => x.trim()).sort();
                                    }
                                    $component.val(valueArray);
                                }
                            } else {
								$component.val(value);
							}
						} else {
							$component.val(""); //HACK-FIX: Not all browsers show the placeholder in Bootstrap 3 if you don't set an empty value.
						}
						
						//Avoid modifications in properties managed by scripts
						if(propertyType.managed || propertyType.dinamic) {
							$component.prop('disabled', true);
						}
						
						var customWidget = profile.customWidgetSettings[propertyType.code];
						var forceDisableRTF = profile.isForcedDisableRTF(propertyType);

                        if(customWidget && !forceDisableRTF) {
                            switch(customWidget) {
                                case 'Word Processor':
                                    if(propertyType.dataType === "MULTILINE_VARCHAR") {
                                        $component = FormUtil.activateRichTextProperties($component, changeEvent(propertyType), propertyType, value, false);
                                        _refreshableFields.push($component);
                                    } else {
                                        alert("Word Processor only works with MULTILINE_VARCHAR data type.");
                                        $("body").on("change", "#"+FormUtil.escapeIdForSelectors($component.attr("id")), changeEvent(propertyType));
                                    }
                                    break;
                                case 'Spreadsheet':
                                    if(propertyType.dataType === "XML") {
                                        var $jexcelContainer = $("<div>", { id: "spreadsheet-" + propertyType.code + "-" + this._viewId});
                                        if(!this._dataSetFormModel.dataSetV3) {
                                            this._dataSetFormModel.dataSetV3 = { properties : {} }
                                        }
                                        JExcelEditorManager.createField($jexcelContainer, this._dataSetFormModel.mode, propertyType.code, this._dataSetFormModel.dataSetV3);
                                        $component = $jexcelContainer;
                                        _refreshableFields.push($component);
                                    } else {
                                        alert("Spreadsheet only works with XML data type.");
                                        $("body").on("change", "#"+FormUtil.escapeIdForSelectors($component.attr("id")), changeEvent(propertyType));
                                    }
                                    break;
                            }
                        } else if(propertyType.dataType === "TIMESTAMP" || propertyType.dataType === "DATE") {
							$("body").on("dp.change", "#"+FormUtil.escapeIdForSelectors($component.attr("id")), changeEvent(propertyType));
						} else {
							$("body").on("change", "#"+FormUtil.escapeIdForSelectors($component.attr("id")), changeEvent(propertyType, isMultiValue));
						}
						
						$controls.append($component);
						
						$fieldset.append($controlGroup);
					}
				}
				propertyGroupPropertiesOnForm++;
			}
			
			if(propertyGroupPropertiesOnForm === 0) {
				$legend.remove();
			}
			
			$wrapper.append($fieldset);
		}
		
		$("#metadataContainer-"+_this._viewId).append($wrapper);
	}

	this.requestOrLockArchiving = function() {
		var _this = this;

		var $window = $('<form>', { 'action' : 'javascript:void(0);' });
		$window.append($('<legend>').append("Request or disallow archiving"));
		var $buttons = $('<div>', {'id' : 'rol_archving_buttons'});
		$window.append($buttons);
		
		var $requestButton = $('<div>', {'class' : 'btn btn-primary btn-secondary', 'text' : 'Request archiving', 'id' : 'request_archiving'});
		$requestButton.click(function() {
			Util.requestArchiving([_this._dataSetFormModel.dataSetV3], Util.unblockUI);
		});
		var $lockButton = $('<div>', {'class' : 'btn btn-primary btn-secondary', 'text' : 'Disallow archiving', 'id' : 'lock_archiving'});
		$lockButton.click(function() {
			_this.lockArchiving();
		});
		var $cancelButton = $('<div>', {'class' : 'btn btn-default', 'text' : 'Cancel', 'id' : 'cancel'});
		$cancelButton.click(function() {
			Util.unblockUI();
		});
		$buttons.append($requestButton).append('&nbsp;').append($lockButton).append('&nbsp;').append($cancelButton);
		
		var css = {
				'text-align' : 'left',
				'top' : '15%',
				'width' : '50%',
				'left' : '15%',
				'right' : '20%',
				'overflow' : 'hidden'
		};
		Util.blockUI($window, css);
	}

	this.lockArchiving = function() {
		var _this = this;

		var $window = $('<form>', { 'action' : 'javascript:void(0);' });
		$window.submit(function() {
		    _this._dataSetFormController.setArchivingLock(true);
		    Util.unblockUI();
		});

		$window.append($('<legend>').append('Disallow archiving'));
		$window.append($('<p>').text("Prevent your dataset from being archived."));

		var $btnAccept = $('<input>', { 'type': 'submit', 'class' : 'btn btn-primary', 'value' : 'Accept' });
		var $btnCancel = $('<a>', { 'class' : 'btn btn-default' }).append('Cancel');
		$btnCancel.click(function() {
		    Util.unblockUI();
		});

		$window.append($btnAccept).append('&nbsp;').append($btnCancel);

		var css = {
		        'text-align' : 'left',
		        'top' : '15%',
		        'width' : '70%',
		        'left' : '15%',
		        'right' : '20%',
		        'overflow' : 'hidden',
				'background' : '#ffffbf'
		};

		Util.blockUI($window, css);
	}

	this.revokeArchivingRequest = function() {
		this._dataSetFormController.setArchivingRequested(false);
	}

	this._allowedToEdit = function() {
		var dataSet = this._dataSetFormModel.v3_dataset;
		var updateAllowed = this._allowedToUpdate(this._dataSetFormModel.rights);
		return updateAllowed && dataSet.frozen == false;
	}
	
	this._allowedToUpdate = function(rights) {
		return rights && rights.rights.indexOf("UPDATE") >= 0;
	}

	this._allowedToMove = function() {
		var dataSet = this._dataSetFormModel.v3_dataset;
		var experiment = dataSet.experiment;
		if (experiment && experiment.frozenForDataSets) {
			return false;
		}
		var sample = dataSet.sample;
		if (sample && sample.frozenForDataSets) {
			return false;
		}
		return this._allowedToUpdate(this._dataSetFormModel.rights);
	}
	
	this._allowedToDelete = function() {
        var dataSet = this._dataSetFormModel.dataSetV3;
        return dataSet.frozen == false && dataSet.type.disallowDeletion == false 
            && this._dataSetFormModel.rights.rights.indexOf("DELETE") >= 0;
	}

	this._getTypeCode = function() {
	    if(this._dataSetFormModel.dataSetV3) {
	        return this._dataSetFormModel.dataSetV3.getType().getCode();
	    }
	}

	this._getDataSetProperty = function(key) {
    	    if(!this._dataSetFormModel.dataSetV3) {
                this._dataSetFormModel.dataSetV3 = { properties : {} };
            }
            this._dataSetFormModel.dataSetV3.properties[key];
    	}

	this._setDataSetProperty = function(key, val) {
	    if(!this._dataSetFormModel.dataSetV3) {
            this._dataSetFormModel.dataSetV3 = { properties : {} };
        }
        this._dataSetFormModel.dataSetV3.properties[key] = val;
	}

	this._initTabHandling = function($header, $container, $form, toolbarModel, rightToolbarModel, altRightToolbarModel) {
		const _this = this;
		const $tabsContainer = $('<div class="tabs-container">');
		const tabs = [
			{ id: "detailsTab-"+_this._viewId, label: "Details", href: "#dataSetFormTab-"+_this._viewId, active: true }
		];
		const $tabsContent = $('<div class="tab-content">');
		const $dataSetFormTab = $('<div id="dataSetFormTab-'+_this._viewId+'" class="tab-pane fade in active"></div>');
		const $afsWidgetTab = $('<div id="afsWidgetTab-'+_this._viewId+'" class="tab-pane fade"></div>');

		$tabsContent.append($dataSetFormTab);
		if (profile.isAFSAvailable()) {
			$tabsContent.append($afsWidgetTab);
			tabs.push({ id: "filesTab-"+_this._viewId, label: "Files", href: "#afsWidgetTab-"+_this._viewId, active: false });
		}
		$tabsContainer.append($tabsContent);

		const tabsModel = {
			containerId: "tabsContainer-"+_this._viewId,
			toolbarId: "toolbar-"+_this._viewId,
			tabs
		};

		const dataSetPermId = this._dataSetFormModel.dataSetV3.permId ? this._dataSetFormModel.dataSetV3.permId.permId : this._dataSetFormModel.dataSetV3.code;
		const $afsWidgetLeftToolBar = this._renderAFSWidgetLeftToolBar($afsWidgetTab, dataSetPermId, "onlyLeftToolbar");
		const $afsWidgetRightToolBar = this._renderAFSWidgetLeftToolBar($afsWidgetTab, dataSetPermId, "onlyRightToolbar");
		const $alternateRightToolbar = FormUtil.getToolbar(altRightToolbarModel);

		const $toolbarWithTabs = FormUtil.getToolbarWithTabs(
			toolbarModel,
			tabsModel,
			rightToolbarModel,
			$afsWidgetRightToolBar,
			$afsWidgetLeftToolBar,
			$alternateRightToolbar
		)
		$header.append($toolbarWithTabs);

		$container.empty().append($tabsContainer);
		$dataSetFormTab.append($form);

		if (this._dataSetFormModel.activeTab) {
			let $activeTabLink = $('#toolbar-' + _this._viewId +' a[data-toggle="tab"][href="' + this._dataSetFormModel.activeTab + '"]');
			if ($activeTabLink.length) {
				$activeTabLink.tab('show');
			}
		} else {
			let $initialActive = $('#toolbar-' + _this._viewId +' a[data-toggle="tab"].active');
			if ($initialActive.length) {
				this._dataSetFormModel.activeTab = $initialActive.attr('href');
			}
		}

		this._updateToolbarForTab(this._dataSetFormModel.activeTab, _this._viewId);
		$("body").on("shown.bs.tab", '#toolbar-' + _this._viewId + ' a[data-toggle="tab"]',  function(e) {
			const target = $(e.target).attr("href");
			_this._dataSetFormModel.activeTab = target;
			_this._updateToolbarForTab(target, _this._viewId);
			if (target === "#afsWidgetTab-"+_this._viewId) {
				if (!$("#afsWidgetTab-"+_this._viewId).data("dssLoaded")) {
					$afsWidgetTab.empty()
					_this._displayAFSWidget($afsWidgetTab, dataSetPermId);
					$("#afsWidgetTab-"+_this._viewId).data("dssLoaded", true);
				}
			}
		});
	};

	this._updateToolbarForTab = function(activeTab, id) {
		if (activeTab === "#afsWidgetTab-"+id) {
			let container = $('#toolbar-' + id);
			container.find(".normal-toolbar-container").hide();
			container.find(".alternate-toolbar-container").show();
			container.find(".normal-right-toolbar-container").hide();
			container.find(".alternate-right-toolbar-container").show();
			container.find(".extra-right-toolbar-container").show();
		} else if(activeTab === "#dataSetFormTab-"+id) {
			let container = $('#toolbar-' + id);
			container.find(".normal-toolbar-container").show();
			container.find(".alternate-toolbar-container").hide();
			container.find(".normal-right-toolbar-container").show();
			container.find(".alternate-right-toolbar-container").hide();
			container.find(".extra-right-toolbar-container").hide();
		}
	};

	this._waitForExtOpenbisInitialization = function() {
		Util.blockUI();
		return new Promise((resolve) => {
			if (this.extOpenbis.initialized === true) {
				Util.unblockUI();
				resolve();
			} else {
				const interval = setInterval(() => {
					if (this.extOpenbis.initialized === true) {
						clearInterval(interval);
						Util.unblockUI();
						resolve();
					}
				}, 100);
			}
		});
	};

	this._displayAFSWidget= function ($container, id) {
		const _this = this
		let $element = $("<div>")

		_this._waitForExtOpenbisInitialization().then(()=> {
			require(["as/dto/rights/fetchoptions/RightsFetchOptions",
				"as/dto/dataset/id/DataSetPermId",
			],
				function (RightsFetchOptions, DataSetPermId) {
					let props = {
						id:id,
						objId: id,
						objKind: "dataset",
						viewType:'list',
						withoutToolbar: true,
						extOpenbis: _this.extOpenbis,
						selectionButtonProps: {
							sx: { textTransform: 'none' }
						}
					}
					let configKey = "AFS-WIDGET-KEY-DS";

					const loadSettings = function () {
						return new Promise(function (resolve) {
							mainController.serverFacade.getSetting(configKey, function (elnGridSettingsStr) {
								var gridSettingsObj = null

								if (elnGridSettingsStr) {
									try {
										var elnGridSettingsObj = JSON.parse(elnGridSettingsStr)

										gridSettingsObj = {
											filterMode: elnGridSettingsObj.filterMode,
											globalFilter: elnGridSettingsObj.globalFilter,
											pageSize: elnGridSettingsObj.pageSize,
											sortings: elnGridSettingsObj.sortings,
											sort: elnGridSettingsObj.sort ? elnGridSettingsObj.sort.sortProperty : null,
											sortDirection: elnGridSettingsObj.sort ? elnGridSettingsObj.sort.sortDirection : null,
											columnsVisibility: elnGridSettingsObj.columns,
											columnsSorting: elnGridSettingsObj.columnsSorting,
											exportOptions: elnGridSettingsObj.exportOptions,
										}
									} catch (e) {
									}
								}
								resolve(gridSettingsObj)
								})
							})
					}


					const onSettingsChange = function (gridSettingsObj) {
						let elnGridSettingsObj = {
							filterMode: gridSettingsObj.filterMode,
							globalFilter: gridSettingsObj.globalFilter,
							pageSize: gridSettingsObj.pageSize,
							sortings: gridSettingsObj.sortings,
							columns: gridSettingsObj.columnsVisibility,
							columnsSorting: gridSettingsObj.columnsSorting,
							exportOptions: gridSettingsObj.exportOptions,
						}

						let elnGridSettingsStr = JSON.stringify(elnGridSettingsObj)
						mainController.serverFacade.setSetting(configKey, elnGridSettingsStr)
					}


					props['onStoreDisplaySettings'] = onSettingsChange;
					props['onLoadDisplaySettings'] = loadSettings;

					let DataBrowser = React.createElement(window.NgComponents.default.DataBrowser, props)

					NgComponentsManager.renderComponent(DataBrowser, $element.get(0));
				}
			);
			$container.append($element);
		})
	}

	this._renderAFSWidgetLeftToolBar= function ($container, id, toolbarTypeIn) {
		let $element = $("<div>")
		const _this = this

		_this._waitForExtOpenbisInitialization().then(()=> {
			require(["as/dto/rights/fetchoptions/RightsFetchOptions",
				"as/dto/dataset/id/DataSetPermId",
			],
				function (RightsFetchOptions, DataSetPermId) {
					let props = {
						owner: id,
						buttonSize: "small",
						toolbarType: toolbarTypeIn,
						viewType:'list',
						className :'btn btn-default',
						primaryClassName :'btn btn-primary',
						selectionButtonProps: {
                                sx: { textTransform: 'none' }
                            },
						extOpenbis: _this.extOpenbis
					}


					let DataBrowserToolbar = React.createElement(window.NgComponents.default.DataBrowserToolbar, props)

					NgComponentsManager.renderComponent(DataBrowserToolbar, $element.get(0));
				}
			);
		});

		return $element;
	}
}
