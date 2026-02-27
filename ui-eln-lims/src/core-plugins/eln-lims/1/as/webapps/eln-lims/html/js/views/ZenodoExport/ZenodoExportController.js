/*
 * Copyright 2011-2026 ETH Zuerich, CISD
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

function ZenodoExportController(parentController) {
    this.exportModel = null;
    this.exportView = null;
    this.zenodoApiTokenKey = parentController.zenodoApiTokenKey;

    this.refresh = function() {
        if(this.exportView) {
            this.exportView.refresh();
        }
    }

    this.init = function(views) {
        this.getSettingValue(this.zenodoApiTokenKey, (function (accessToken) {
            if (accessToken && accessToken !== '') {
                this.exportModel = new ZenodoExportModel(accessToken);
                this.exportView = new ZenodoExportView(this, this.exportModel);
                this.exportView.repaint(views);
            } else {
                Util.showError('Personal Zenodo API Token missing, please set it in your user profile.');
            }
        }).bind(this));
    };

    this._addNodeToList = function(node, list, _viewId) {
        list.push({
            kind: node.data.entityType,
            permId: node.key,

            withLevelsAbove: $("#LEVELS-ABOVE-EXPORT-"+_viewId).is(":checked"),
            withLevelsBelow: $("#LEVELS-BELOW-EXPORT-"+_viewId).is(":checked"),
            withObjectsAndDataSetsParents: $("#PARENTS-EXPORT-"+_viewId).is(":checked"),
            withObjectsAndDataSetsChildren: $("#CHILDREN-EXPORT-"+_viewId).is(":checked"),
            withObjectsAndDataSetsOtherSpaces: $("#OTHER-SPACES-EXPORT-"+_viewId).is(":checked"),
        })
    }

    this.exportSelected = function() {
        var _this = this;
        var _viewId = this.exportView._viewId;
        var selectedNodes = $(this.exportModel.tree).fancytree('getTree').getSelectedNodes();
        var title = this.exportView.$titleTextBox.val().trim();
        var fileName = this.exportView.$fileNameBox.val().trim();

        var groupRows = this.exportModel.tableModel.getValues();
        var nameColumn = this.exportModel.tableModel.columns[0];
        var valueColumn = this.exportModel.tableModel.columns[1];
        var checkedGroups = groupRows.flatMap(row => row[valueColumn.label] ? [row[nameColumn.label]] : []);

        var nodeExportList = [];
        for (var eIdx = 0; eIdx < selectedNodes.length; eIdx++) {
            var node = selectedNodes[eIdx];
            if(node.data.entityType === "ROOT") {
                for(var id = 0; id < node.children.length; id++) {
                    var childNode = node.children[id];
                    this._addNodeToList(childNode, nodeExportList, _viewId);
                }
                break;
            }
        }

        for (var eIdx = 0; eIdx < selectedNodes.length; eIdx++) {
            var node = selectedNodes[eIdx];
            if(node.data.entityType !== "ROOT") {
                this._addNodeToList(node, nodeExportList, _viewId);
            }
        }

		var toExportModel = {
			nodeExportList: nodeExportList,
			withEmail: false,
			withImportCompatibility: $("#COMPATIBLE-IMPORT-"+_viewId).is(":checked"), //COMPATIBLE-IMPORT
			formats: {
				pdf: $("#PDF-EXPORT-"+_viewId).is(":checked"), //PDF-EXPORT
				xlsx: $("#XLSX-EXPORT-"+_viewId).is(":checked"), //XLSX-EXPORT
				data: $("#DATA-EXPORT-"+_viewId).is(":checked"), //DATA-EXPORT
				afsData: $("#FILES-EXPORT-"+_viewId).is(":checked") //DATA-EXPORT
			}
		}

        if (toExportModel.nodeExportList.length === 0) {
            Util.showInfo('First select something to export.');
        } else if (title === "") {
            Util.showInfo('Please enter a title.');
        } else if (!this.isValid(toExportModel.nodeExportList)) {
            Util.showInfo('Not only spaces and the root should be selected. It will result in an empty export file.');
        } else if (groupRows.length > 0 && checkedGroups.length === 0) {
            Util.showInfo('At least one group should be selected.');
        } else {
            Util.blockUI();

            for (var i = 0; i < checkedGroups.length; i++) {
                var group = checkedGroups[i];
                toExportModel.nodeExportList.push({type: 'GROUP', permId: 'GROUP:' + group, expand: null});
            }

            this.getUserInformation((function(userInformation) {
                mainController.serverFacade.exportZenodoAs(toExportModel, userInformation, title, fileName, this.exportModel.accessToken,
                        function(serviceResult) {
                            Util.unblockUI();
                            var zenodoResultUrl = serviceResult.result.url;
                            if(zenodoResultUrl) {
                                var win = window.open(zenodoResultUrl, '_blank');
                                win.focus();
                                mainController.refreshView();
                            }
                        });
            }).bind(this));
        }
    };

    this.isValid = function(nodeExportList) {
        for (var i = 0; i < nodeExportList.length; i++) {
            var value = nodeExportList[i];
            if (value.kind !== 'SPACE' || value.withLevelsBelow) {
                return true;
            }
        }
        return false;
    };

    this.waitForOpExecutionResponse = function(operationExecutionPermIdString, callbackFunction) {
        var _this = this;
        require(['as/dto/operation/id/OperationExecutionPermId',
                'as/dto/operation/fetchoptions/OperationExecutionFetchOptions'],
            function(OperationExecutionPermId, OperationExecutionFetchOptions) {
                var operationExecutionPermId = new OperationExecutionPermId(operationExecutionPermIdString);
                var fetchOptions = new OperationExecutionFetchOptions();
                var fetchOptionsDetails = fetchOptions.withDetails();
                fetchOptionsDetails.withResults();
                fetchOptionsDetails.withError();
                mainController.openbisV3.getOperationExecutions([operationExecutionPermId], fetchOptions).done(function(results) {
                    var result = results[operationExecutionPermIdString];
                    var v2Result = null;
                    if (result && result.details && result.details.results) {
                        v2Result = result.details.results[0];
                    }

                    if (result && result.state === 'FINISHED') {
                        mainController.serverFacade.customELNApiCallbackHandler(v2Result, callbackFunction);
                    } else if (!result || result.state === 'FAILED') {
                        mainController.serverFacade.customELNApiCallbackHandler(v2Result, callbackFunction);
                    } else {
                        setTimeout(function() {
                            _this.waitForOpExecutionResponse(operationExecutionPermIdString, callbackFunction);
                        }, 3000);
                    }
                });
            });
    };

    this.getUserInformation = function(callback) {
        var userId = mainController.serverFacade.getUserId();
        mainController.serverFacade.getSessionInformation(function(sessionInfo) {
            var userInformation = {
                id: userId,
            };
            callback(userInformation);
        });
    };

    this.getSettingValue = function (key, callback) {
        parentController.serverFacade.getSetting(key, callback);
    };
}