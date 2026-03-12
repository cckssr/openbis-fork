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
function RoCrateExportController(parentController) {
    this.exportModel = null;
    this.exportView = null;

    var _this = this;

    this.refresh = function() {
        if(this.exportView) {
            this.exportView.refresh();
        }
    }

    this.init = function(views) {

        this.getSettingValue("ro-crate-job-ids", (function(jobsStr) {
            _this.exportModel = new RoCrateExportModel();
            _this.exportView = new RoCrateExportView(this, _this.exportModel);
            if(jobsStr) {
                var jobs = JSON.parse(jobsStr);
                _this.exportModel.jobs = jobs;
                _this.checkStatues();
            }
            _this.exportView.repaint(views);
        }).bind(this));


    };

    this.checkStatues = function() {
        mainController.serverFacade.statusRoCrateJobs(function(output) {
            if(output.error) {
                if(output.error.message) {
                    Util.showError(output.error.message);
                } else {
                    Util.showError(output.error);
                }
            } else {
                var result = output.result;
                if(result.jobs) {
                    for(var i = 0; i < _this.exportModel.jobs.length; i++) {
                        var found = false;
                        for(var j = 0; j < result.jobs.length; j++) {
                            if(_this.exportModel.jobs[i].jobId === result.jobs[j].jobId) {
                                found = true;
                                _this.exportModel.jobs[i].status = result.jobs[j].status;
                                _this.exportModel.jobs[i].errors = result.jobs[j].errors;
                                break;
                            }
                        }
                        if(!found) {
                            _this.exportModel.jobs[i].status = "DELETED";
                        }
                    }
                    _this.updateJobs();
                    _this.exportView.dataGrid.refresh();
                }
            }
        });
    }

    this.updateJobs = function() {
        var stringy = JSON.stringify(_this.exportModel.jobs)
        _this.setSettingValue("ro-crate-job-ids", stringy);
    }

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
            }
//                withImportCompatibility: $("#COMPATIBLE-IMPORT").is(":checked"), //COMPATIBLE-IMPORT
        }

        if (nodeExportList.length === 0) {
            Util.showInfo("First select something to export.");
        } else {
            Util.blockUI();

            mainController.serverFacade.exportRoCrate(exportModel, function (result) {
                if (result.error) {
                    if(result.error.message) {
                        Util.showError(result.error.message);
                    } else {
                        Util.showError(result.error);
                    }
                } else {
                    var data = result.result;
                    _this.exportModel.jobs.push({
                        jobId: data.jobId,
                        status: "SCHEDULED",
                        time: Date.now()
                    });
                    _this.updateJobs();
                    Util.showSuccess("Export is being processed.", function () { Util.unblockUI(); });

                    mainController.refreshView();
                }
            });
        }

    }

    this.getSettingValue = function (key, callback) {
        parentController.serverFacade.getSetting(key, callback);
    };

    this.setSettingValue = function (key, value) {
        parentController.serverFacade.setSetting(key, value);
    };

}