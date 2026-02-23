/*
 * Copyright 2016 ETH Zuerich, Scientific IT Services
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

function ExportTreeController(parentController) {
	var parentController = parentController;
	var exportTreeModel = new ExportTreeModel();
	var exportTreeView = new ExportTreeView(this, exportTreeModel);

	this.refresh = function() {
        if(exportTreeView) {
            exportTreeView.refresh();
        }
    }

	this.init = function (views) {
		exportTreeView.repaint(views);
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

	this.exportSelected = function () {
	    var _viewId = exportTreeView._viewId;
		var selectedNodes = $(exportTreeModel.tree).fancytree('getTree').getSelectedNodes();

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

		var exportModel = {
			nodeExportList: nodeExportList,
			withEmail: false,
			withImportCompatibility: $("#COMPATIBLE-IMPORT-"+_viewId).is(":checked"), //COMPATIBLE-IMPORT
			formats: {
				pdf: $("#PDF-EXPORT-"+_viewId).is(":checked"), //PDF-EXPORT
				xlsx: $("#XLSX-EXPORT-"+_viewId).is(":checked"), //XLSX-EXPORT
				data: $("#DATASET-EXPORT-"+_viewId).is(":checked"), //DATA-EXPORT
				afsData: $("#FILES-EXPORT-"+_viewId).is(":checked") //DATA-EXPORT
			}
		}

		if (nodeExportList.length === 0) {
			Util.showInfo("First select something to export.");
		} else {
			Util.blockUI();
			mainController.serverFacade.exportAll(exportModel, function (result) {
				if (result.error) {
					Util.showError(result.error);
				} else {
					Util.showSuccess("Export is being processed, you will receive an email when it is finished. If you logout the process will stop.", function () { Util.unblockUI(); });
					mainController.refreshView();
				}
			});
		}

	}
}