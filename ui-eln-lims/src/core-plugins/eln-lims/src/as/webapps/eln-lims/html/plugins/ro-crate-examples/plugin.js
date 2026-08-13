function RoCrateExamplesPluginViewTechnology() {
this.init();
}

$.extend(RoCrateExamplesPluginViewTechnology.prototype, ELNLIMSPlugin.prototype, {

    plugin: {
        icon : "fa fa-bug",
        uniqueViewName : "RO_CRATE_EXAMPLES",
        label : "Ro-Crate Examples",
        isHidden: function() {
            var pluginThis = this;
            if(typeof this.hidden !== 'undefined') {
                return this.hidden;
            }

            mainController.serverFacade.customASService({
                "method" : "isEnabled",
            }, function(result) {
                if(result.result) {
                    pluginThis.hidden = false;
                } else {
                    pluginThis.hidden = true;
                }
            }, "ro-crate-examples", (e) => {
                console.log("there were errors:" + e);
                pluginThis.hidden = true;
            });
            return true;
        },
        paintView : function($header, $content) {

            var $icon = $("<span/>");
            $icon.addClass("fa fa-bug")

            $header
                .append($("<h1>").append($icon).append(" RO-Crate Examples"))
                .append($("<div>").append("This is page with example data imports for RO-Crate tests"));

            var scicat = $("<div>");
            scicat.append($('<br>'));

            var modeLabel = FormUtil.createLabel('SciCat data import');
            scicat.append(modeLabel);

            var $ok = FormUtil.getButtonWithText("Import", function() {

                var callbackFunction = function(result) {
                    var aaa = result;
                    if(result.error) {
                        Util.showError(result.message, function() {}, true);
                    } else {
                        Util.showSuccess("Import successful", function () { Util.unblockUI(); });
                    }
                }

                Util.blockUI();
                mainController.serverFacade.customASService({
                    "method" : "scicat",
                }, callbackFunction, "ro-crate-examples", null);

            });
            scicat.append($ok);

            $content.append(scicat);

        }
    },
    init: function() {

    },
    getExtraUtilities : function() {
        return [this.plugin];
    }
});

profile.plugins.push(new RoCrateExamplesPluginViewTechnology());