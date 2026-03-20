/*
 * Copyright 2026 ETH Zuerich, Scientific IT Services
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
function SciCatExportController(parentController) {
    this.exportModel = null;
    this.exportView = null;
    this.sciCatApiTokenKey = parentController.sciCatApiTokenKey;

    var _this = this;

    this.refresh = function() {
        if(this.exportView) {
            this.exportView.refresh();
        }
    }

    this.init = function(views) {
        this.getSettingValue("personal-sci-cat-api-token", (function(accessToken) {
            if (accessToken && accessToken !== '') {
                _this.exportModel = new SciCatExportModel(accessToken);
                _this.exportView = new SciCatExportView(this, _this.exportModel);
                _this.exportView.repaint(views);
            } else {
                Util.showError('Personal Sci Cat API Token missing, please set it in your user profile.');
            }
        }).bind(this));
    };


    this._addNodeToList = function(node, list) {
        list.push({
            kind: node.data.entityType,
            permId: node.key,
        })
    }

    this.exportSelected = function() {
        var _viewId = _this.exportView._viewId;
        var selectedNodes = $(_this.exportModel.tree).fancytree('getTree').getSelectedNodes();

        var nodeExportList = [];
        for (var eIdx = 0; eIdx < selectedNodes.length; eIdx++) {
            var node = selectedNodes[eIdx];
            if(node.data.entityType === "ROOT") {
                for(var id = 0; id < node.children.length; id++) {
                    var childNode = node.children[id];
                    this._addNodeToList(childNode, nodeExportList);
                }
                break;
            }
        }

        for (var eIdx = 0; eIdx < selectedNodes.length; eIdx++) {
            var node = selectedNodes[eIdx];
            if(node.data.entityType !== "ROOT") {
                this._addNodeToList(node, nodeExportList);
            }
        }

        var derived = _this.exportView.$derivedBox.val().trim();
        var metaDataValues = _this.exportView.tableModel.getValues().filter(x => x.key !== '');

        var exportModel = {
            nodeExportList: nodeExportList,
            withLevelsBelow: $("#LEVELS-BELOW-EXPORT-"+_viewId).is(":checked"),
            withObjectsAndDataSetsParents: $("#PARENTS-EXPORT-"+_viewId).is(":checked"),
            withObjectsAndDataSetsChildren: $("#CHILDREN-EXPORT-"+_viewId).is(":checked"),
            withObjectsAndDataSetsOtherSpaces: $("#OTHER-SPACES-EXPORT-"+_viewId).is(":checked"),
            formats: {
                pdf: $("#PDF-EXPORT-"+_viewId).is(":checked"), //PDF-EXPORT
                xlsx: $("#XLSX-EXPORT-"+_viewId).is(":checked"), //XLSX-EXPORT
                data: $("#DATASET-EXPORT-"+_viewId).is(":checked"), //DATA-EXPORT
                afsData: $("#FILES-EXPORT-"+_viewId).is(":checked") //DATA-EXPORT
            },
            derived: derived,
            metaData: metaDataValues
//                withImportCompatibility: $("#COMPATIBLE-IMPORT").is(":checked"), //COMPATIBLE-IMPORT
        }

        if (nodeExportList.length === 0) {
            Util.showInfo("First select something to export.");
        } else {
            Util.blockUI();

            mainController.serverFacade.exportSciCat(exportModel, _this.exportModel.accessToken, function (result) {
                if (result.error) {
                    if(result.error.message) {
                        Util.showError(result.error.message);
                    } else {
                        Util.showError(result.error);
                    }
                } else {
                    Util.showSuccess("Export is being processed." + result.result, function () { Util.unblockUI(); });
                    mainController.refreshView();
                }
            });
        }

    }

    this.getSettingValue = function (key, callback) {
        parentController.serverFacade.getSetting(key, callback);
    };


}