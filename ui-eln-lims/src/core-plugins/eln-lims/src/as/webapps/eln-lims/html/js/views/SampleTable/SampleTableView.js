/* Copyright 2014 ETH Zuerich, Scientific IT Services
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

function SampleTableView(sampleTableController, sampleTableModel) {
	this._sampleTableController = sampleTableController;
	this._sampleTableModel = sampleTableModel;
	this._tableContainer = $("<div>", { id : "table-container"});
	this.sampleTypeSelector = null;
	this._viewId = mainController.getNextId();
	var _refreshableFields = [];

	this.refresh = function() {
	    var dropdownId = '#all-sample-types-' + this._viewId;
	    var allTypesDropdown = $(dropdownId);
	    if(allTypesDropdown.length !== 0) {
	        Select2Manager.add(allTypesDropdown);
	    }
	    for(var field of _refreshableFields) {
            field.refresh();
        }

	}
	
	this.repaint = function(views) {
		var $container = views.content;
        mainController.profile.beforeViewPaint(ViewType.SAMPLE_TABLE, this._sampleTableModel, $container);
		var _this = this;
		
		var $title = $("<div>");
		if(this._sampleTableModel.title && this._sampleTableModel.experimentIdentifier) {
            var titlePrefix = Util.getDisplayNameFromCode(this._sampleTableModel.experiment.experimentTypeCode) + ": ";
            var title = titlePrefix + IdentifierUtil.getCodeFromIdentifier(this._sampleTableModel.experimentIdentifier);
			if(this._sampleTableModel.experiment && this._sampleTableModel.experiment.properties[profile.propertyReplacingCode]) {
                title = titlePrefix + this._sampleTableModel.experiment.properties[profile.propertyReplacingCode];
			}
			
			var spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(this._sampleTableModel.experimentIdentifier);
			var projectCode = IdentifierUtil.getProjectCodeFromExperimentIdentifier(this._sampleTableModel.experimentIdentifier);
			var experimentCode = IdentifierUtil.getCodeFromIdentifier(this._sampleTableModel.experimentIdentifier);

			$title.append($("<h2>").append(title));
		} else if(this._sampleTableModel.title) {
			$title.append($("<h2>").append(this._sampleTableModel.title));
		}
		
		//
		// Toolbar
		//
		var toolbarModel = [];
		if(this._sampleTableModel.experimentIdentifier) {
			var experimentSpace = IdentifierUtil.getSpaceCodeFromIdentifier(this._sampleTableModel.experimentIdentifier);
			var experimentCode = IdentifierUtil.getCodeFromIdentifier(this._sampleTableModel.experimentIdentifier);

			//
			if(this._sampleTableModel.experiment && 
					this._sampleTableModel.experiment.properties &&
					this._sampleTableModel.experiment.properties[profile.getInternalNamespacePrefix() + "DEFAULT_OBJECT_TYPE"]) {
				this._sampleTableModel.sampleTypeCodeToUse = this._sampleTableModel.experiment.properties[profile.getInternalNamespacePrefix() + "DEFAULT_OBJECT_TYPE"];
			}
			
			var sampleTypeCodeToUse = this._sampleTableModel.sampleTypeCodeToUse;
			
			//Add Sample Type
			if(sampleTypeCodeToUse !== null & _this._sampleTableModel.sampleRights.rights.indexOf("CREATE") >= 0) {
				var $createButton = FormUtil.getToolbarButton("ENTRY", function() {
					Util.blockUI();
                    setTimeout(function() {
                        var argsMap = {
                            "sampleTypeCode" : sampleTypeCodeToUse,
                            "experimentIdentifier" : _this._sampleTableModel.experimentIdentifier
                        };
                        mainController.changeView("showCreateSubExperimentPage", JSON.stringify(argsMap));
                    }, 100);
				}, Util.getDisplayNameFromCode(sampleTypeCodeToUse), null, "create-btn-"+_this._viewId, 'btn btn-primary btn-secondary');
				
				toolbarModel.push({ component : $createButton });
			}
		}
		
		var tableToolbarModel = [];
		if(this._sampleTableModel.experimentIdentifier) {
			var $options = this._getOptionsMenu();
			toolbarModel.push({ component : $options, tooltip: null });
		} else if(this._sampleTableModel.projectPermId) {

		} else {
			var $allSampleTypes = this._getAllSampleTypesDropdown();
			tableToolbarModel.push({ component : $allSampleTypes, tooltip: null });
			var $options = this._getOptionsMenu();
			tableToolbarModel.push({ component : $options, tooltip: null });
		}
		
		var $header = views.header;
		$header.append($title);
		
		if(toolbarModel.length > 0) {
			$header.append(FormUtil.getToolbar(toolbarModel));
		}
		if(tableToolbarModel.length > 0) {
			$header.append(FormUtil.getToolbar(tableToolbarModel));
		}
		
		$container.append(this._tableContainer);
        mainController.profile.afterViewPaint(ViewType.SAMPLE_TABLE, this._sampleTableModel, $container);
	}
	
	this.getTableContainer = function() {
		return this._tableContainer;
	}
	
	//
	// Menus
	//
	this._getOptionsMenu = function() {
		var _this = this;
		var $dropDownMenu = $("<span>", { class : 'dropdown' });
		var $caret = $("<a>", { 'href' : '#', 'data-toggle' : 'dropdown', class : 'dropdown-toggle btn btn-default', 'id' : 'sample-options-menu-btn'})
		$caret.append("More ... ").append($("<b>", { class : 'caret' }));
		var $list = $("<ul>", { class : 'dropdown-menu', 'role' : 'menu', 'aria-labelledby' :'sampleTableDropdown' });
		$dropDownMenu.append($caret);
		$dropDownMenu.append($list);
		
		if(_this._sampleTableModel.experimentIdentifier && _this._sampleTableModel.sampleRights.rights.indexOf("CREATE") >= 0) {
			var $createSampleOption = $("<li>", { 'role' : 'presentation' }).append($("<a>", {'title' : 'New ' + ELNDictionary.Sample + '', 'id' : 'create-' + ELNDictionary.Sample.toLowerCase() + '-btn'}).append('New ' + ELNDictionary.Sample + ''));
			var createSampleFunction = function() {
                _this.createNewSample(_this._sampleTableModel.experimentIdentifier);
            }
			$createSampleOption.click(createSampleFunction);
			$createSampleOption.refresh = function() {
                this.off('click')
                this.click(createSampleFunction);
            }
			_refreshableFields.push($createSampleOption);
			$list.append($createSampleOption);
		}
		
		   //
        var label = "XLS Batch Register " + ELNDictionary.Samples;
        var id = 'xsl-register-' + ELNDictionary.Sample.toLowerCase() + '-btn-'+_this._viewId;
        var $xslBatchRegisterOption = $("<li>", { 'role' : 'presentation' }).append($("<a>", {'title' : label, 'id' : id}).append(label));
        var batchRegisterFunction = function() {
           _this._sampleTableController.registerSamples(_this._sampleTableModel.experimentIdentifier);
        }
        $xslBatchRegisterOption.click(batchRegisterFunction);
        $xslBatchRegisterOption.refresh = function() {
            this.off('click')
            this.click(batchRegisterFunction);
        }
        _refreshableFields.push($xslBatchRegisterOption);
        $list.append($xslBatchRegisterOption);

        var label = "XLS Batch Update " + ELNDictionary.Samples;
        var id = 'xsl-update-' + ELNDictionary.Sample.toLowerCase() + '-btn';
        var $xslBatchUpdateOption = $("<li>", { 'role' : 'presentation' }).append($("<a>", {'title' : label, 'id' : id}).append(label));
        var batchUpdateFunction = function() {
          _this._sampleTableController.updateSamples(_this._sampleTableModel.experimentIdentifier);
        }
        $xslBatchUpdateOption.click(batchUpdateFunction);
        $xslBatchUpdateOption.refresh = function() {
            this.off('click')
            this.click(batchUpdateFunction);
        }
        _refreshableFields.push($xslBatchUpdateOption);
        $list.append($xslBatchUpdateOption);
		
		if(_this._sampleTableModel.experimentIdentifier) {
            var expKindName = ELNDictionary.getExperimentKindName(_this._sampleTableModel.experiment.experimentTypeCode, false);
			var $searchCollectionOption = $("<li>", { 'role' : 'presentation' }).append($("<a>", {'title' : 'Search in ' + expKindName, 'id' : 'search-' + ELNDictionary.Sample.toLowerCase() + '-btn'}).append('Search in ' + expKindName));
			var searchCollectionFunction = function() {

                var sampleRules = { "UUIDv4" : { type : "Experiment", name : "ATTR.PERM_ID", value : _this._sampleTableModel.experiment.permId } };
                var rules = { entityKind : "SAMPLE", logicalOperator : "AND", rules : sampleRules };

                mainController.changeView("showAdvancedSearchPage", JSON.stringify(rules));
            }
			$searchCollectionOption.click(searchCollectionFunction);
			$searchCollectionOption.refresh = function() {
                this.off('click')
                this.click(searchCollectionFunction);
            }

			_refreshableFields.push($searchCollectionOption);
			$list.append($searchCollectionOption);

			var $detailsOption = $("<li>", { 'role' : 'presentation' }).append($("<a>", {'title' : 'Edit Collection', 'id' : 'detail-btn'}).append('Edit Collection'));
            var detailsOptionFunction = function() {
                mainController.changeView("showExperimentPageFromIdentifier", encodeURIComponent('["' +
                        _this._sampleTableModel.experimentIdentifier + '",true]'));
            }
            $detailsOption.click(detailsOptionFunction);
            $detailsOption.refresh = function() {
                this.off('click')
                this.click(detailsOptionFunction);
            }
            _refreshableFields.push($detailsOption);
            $list.append($detailsOption);
		}
		
		return $dropDownMenu;
	}
	
	this._getAllSampleTypesDropdown = function() {
		var _this = this;

		var $sampleTypesSelector = $sampleTypesSelector = FormUtil.getSampleTypeDropdown('all-sample-types-'+_this._viewId, false, ["STORAGE", "STORAGE_POSITION"], null, "*"); // This should return all types allowed by all *_ELN_SETTINGS

        $("body").on("change", '#all-sample-types-'+_this._viewId, function() {
			var sampleTypeToShow = $(this).val();
			
			var advancedSampleSearchCriteria = {
					entityKind : "SAMPLE",
					logicalOperator : "AND",
					rules : { "1" : { type : "Attribute", name : "SAMPLE_TYPE", value : sampleTypeToShow } }
			}
			
			_this._sampleTableController._reloadTableWithAllSamples(advancedSampleSearchCriteria);
		});
		
		return $("<span>").append($sampleTypesSelector);
	}
	
	//
	// Menu Operations
	//
	this.createNewSample = function(experimentIdentifier) {
	    FormUtil.createNewSample(experimentIdentifier);
	}

}