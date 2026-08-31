function RoCrateExamplesPluginViewTechnology() {
this.init();
}

$.extend(RoCrateExamplesPluginViewTechnology.prototype, ELNLIMSPlugin.prototype, {

    plugin: {
        icon : "fa fa-bug",
        uniqueViewName : "RO_CRATE_EXAMPLES",
        label : "Ro-Crate Examples",
        isHidden: function(callback) {
            return new Promise((resolve) => {
                mainController.serverFacade.customASService({
                    "method" : "isEnabled",
                }, (result) => {

                    if(result.result) {
                        callback(false);
                    } else {
                        callback(true);
                    }
                    resolve(result);
                }, "ro-crate-examples", (error) => {
                    console.log(error);
                    resolve(result);
                });
            });
        },
        paintView : function($header, $content) {

            var $icon = $("<span/>");
            $icon.addClass("fa fa-bug")

            $header
                .append($("<h1>").append($icon).append(" RO-Crate Examples"))
                .append($("<div>").append("This is page with example data imports for RO-Crate tests"));

            var addButton = function(container, methodName) {
                var $div = $("<div>");
                $div.append($('<br>'));

                var modeLabel = FormUtil.createLabel(methodName + ' data import');
                $div.append(modeLabel).append(" ");

                var $execute = FormUtil.getButtonWithText("execute", function() {

                    var callbackFunction = function(result) {
                        if(result.error) {
                            Util.showError(result.message, function() {}, true);
                        } else {
                            Util.showSuccess("Import successful", function () { Util.unblockUI(); });
                        }
                    }

                    Util.blockUI();
                    mainController.serverFacade.customASService({
                        "method" : methodName,
                    }, callbackFunction, "ro-crate-examples", null);

                });
                var $sources = FormUtil.getButtonWithText("sources", function() {

                    var callbackFunction = function(result) {
                        if(result.error) {
                            Util.showError(result.message, function() {}, true);
                        } else {
                            Util.unblockUI();
                            if(result) {
                                window.open(result, "_blank");
                            }
                        }
                    }

                    Util.blockUI();
                    mainController.serverFacade.customASService({
                        "method" : 'download',
                        "type" : methodName,
                    }, callbackFunction, "ro-crate-examples", null);

                });
                $div.append($execute).append(" ");
                $div.append($sources);
                container.append($div);
            };

            var addRoCrateImportButton = function(container, methodName) {
                var $div = $("<div>");
                $div.append($('<br>'));

                var modeLabel = FormUtil.createLabel(methodName + ' data import');
                $div.append(modeLabel).append(" ");

                var $execute = FormUtil.getButtonWithText("execute", function() {

                    var pollForResult = function(jobId) {
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

                    var callbackFunction = function(result) {
                        if(result.error) {
                            Util.showError(result.message, function() {}, true);
                        } else {

                            var parameters = {
                                "importMode": 'UPDATE_IF_EXISTS',
                                "fileName": result.result,
                                "projectIdentifier": '/DEFAULT/DEFAULT',
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
                                        pollForResult(jobId);
                                    }
                                });
                        }
                    }

                    Util.blockUI();
                    mainController.serverFacade.customASService({
                        "method" : 'getData',
                        "type" : methodName,
                    }, callbackFunction, "ro-crate-examples", null);

                });


                var $sources = FormUtil.getButtonWithText("sources", function() {

                    var callbackFunction = function(result) {
                        if(result.error) {
                            Util.showError(result.message, function() {}, true);
                        } else {
                            Util.unblockUI();
                            if(result) {
                                window.open(result, "_blank");
                            }
                        }
                    }

                    Util.blockUI();
                    mainController.serverFacade.customASService({
                        "method" : 'download',
                        "type" : methodName,
                    }, callbackFunction, "ro-crate-examples", null);

                });
                $div.append($execute).append(" ");
                $div.append($sources);
                container.append($div);
            }

            addButton($content, 'scicat');
            addRoCrateImportButton($content, 'publication');
            addRoCrateImportButton($content, 'logbook');

        }
    },
    init: function() {

    },
    getExtraUtilities : function() {
        return [this.plugin];
    }
});

profile.plugins.push(new RoCrateExamplesPluginViewTechnology());