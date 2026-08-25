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
function RoCrateImportController(parentController) {
    this.importModel = null;
    this.importView = null;

    var _this = this;

    this.refresh = function() {
        if(this.importView) {
            this.importView.refresh();
        }
    }

    this.init = function(views) {
        _this.importModel = new RoCrateImportModel();
        _this.importView = new RoCrateImportView(this, _this.importModel);
        _this.importView.repaint(views);
    };

    this.importSelected = function() {
        var _this = this;
        var entity = _this.importModel.entity;
        if(_this.importModel.file === null) {
            Util.showInfo("Upload file for import first.");
        } else if(!entity) {
            Util.showInfo("Select fallback project to import entities.");
        } else {
            var selectedValue = $('#importModeDropdown-roCrate').val();
            var importMode = "UPDATE_IF_EXISTS";
            switch (selectedValue) {
                case 'fail':
                    importMode = "FAIL_IF_EXISTS";
                    break;
                case 'ignore':
                    importMode = "IGNORE_IF_EXISTS";
                    break;
                case 'update':
                    importMode = "UPDATE_IF_EXISTS";
                    break;
            }

            Util.blockUI();
            mainController.openbisV3.uploadToSessionWorkspace(this.importModel.file)
                .done(function () {
                    var parameters = {
                        "importMode": importMode,
                        "fileName": _this.importModel.file.name,
                        "projectIdentifier": entity.identifier.identifier,
                    }
                    mainController.serverFacade.importRoCrate(parameters,
                        function (result) {
                            if (result.error) {
                                if (result.error.message) {
                                    Util.showError(result.error.message);
                                } else {
                                    Util.showError(result.error);
                                }
                            } else {
                                var jobId = result.result.jobId;
                                _this.pollForResult(jobId);
                            }
                        });
                });
        }
    }

    this.pollForResult = function(jobId) {
        var waitUntilDone = null;
        waitUntilDone = function() {
            mainController.serverFacade.statusRoCrateImport(jobId, function(pollResults) {
                if (pollResults.error) {
                    if(pollResults.error.message) {
                        Util.showError(pollResults.error.message);
                    } else {
                        Util.showError(pollResults.error);
                    }
                } else {
                    var result = pollResults.result;
                    if(result.status === "COMPLETED") {
                        if(result.validationResult && result.validationResult.isValid === false) {
                            Util.showError("Import failed to validate:\n" + JSON.stringify(result.validationResult.errors, null, "\t"));
                        } else {
                            Util.showSuccess("Import is completed.", function () { Util.unblockUI(); });
                            mainController.refreshView();
                        }
                    } else if(result.status === "FAILED") {
                        Util.showError(result.errors);
                    } else {
                        setTimeout(waitUntilDone, 1000);
                    }
                }

            });
        }
        waitUntilDone();
    }




}