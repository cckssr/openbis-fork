/*
 * Copyright 2024 ETH Zuerich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function InstanceSettingsView(instanceSettingsController, instanceSettingsModel) {
	this._instanceSettingsController = instanceSettingsController;
	this._instanceSettingsModel = instanceSettingsModel;
	this.settingsContainer = $("<div>");
	this.commentsAddButton = $("<div>");


    this.repaint = function($container) {

        var tableModel = this._getTableModel();
        tableModel.fullWidth = false;

        // define columns
        tableModel.columns = [{ label : "Setting", width: "20%"}, { label : "Value", width: "80%"}];
        tableModel.rowBuilders = {
            "Setting" : function(rowData, rowType) {
                return $("<span>").text(rowData.label).attr('varname', rowData.varname);
            },
            "Value" : function(rowData, rowType) {
                if(rowType === "BOOLEAN") {
                    var $checkbox = $("<input>", { type : "checkbox", name : "cb" });
                        if (rowData.value) {
                            $checkbox.attr("checked", true);
                        }
                        return $checkbox;
                } else if(rowType === "INTEGER") {
                    var $number = FormUtil._getNumberInputField(rowData.label, '', '1', false);
                    $number.attr("value", rowData.value);
                    return $number;
                } else if(rowType === "REGEX") {
                    var $regex = FormUtil._getInputField("text", rowData.label, '', null, false);
                    $regex.attr("value", rowData.value);
                    return $regex;
                } else if(rowType === "STRING") {
                    var $text = FormUtil._getInputField("text", rowData.label, '', null, false);
                    $text.attr("value", rowData.value);
                    return $text;
                } else {
                    return null;
                }
            }
        };

        // add data
        for(setting of this._instanceSettingsModel.getSettings()) {
            tableModel.addRow({
                label: setting.label,
                value: this._getValueForSetting(setting.variableName, setting.type, setting.default),
                varname : setting.variableName
            }, setting.type);
        }


        // transform output
        tableModel.valuesTransformer = function(values) {
            var settings = {};
            for (var value of values) {
                settings[value["Setting"]] = value["Value"];
            }
            return settings;
        };

        $container.append(this._getTable(tableModel));
        this._tableModel = tableModel;
    }

    this.getValues = function() {
        return this._tableModel.getValues();
    }

    this._getValueForSetting = function(variableName, type, defaultValue) {
        var profileToEdit = this._instanceSettingsModel.profileToEdit;
        var mainProfile = mainController.profile;
        if(profileToEdit[variableName]) {
            return profileToEdit[variableName];
        }
        if(mainProfile[variableName]) {
            return mainProfile[variableName];
        }
        return defaultValue;
    }

    this._getTableModel = function() {
        var tableModel = {};
        tableModel.columns = []; // array of elements with label and optional width
        tableModel.rowBuilders = {}; // key (column name); value (function to build widget)
        tableModel.rows = []; // array of maps with key (column name); value (widget)
        tableModel.rowExtraBuilder = null; // optional builder for expandable component per row
        tableModel.rowExtras = []; // array of extras corresponding to the rows
        tableModel.rowExtraModels = [] // row extra models can be placed here. models need getValues() function
        tableModel.dynamicRows = false; // allows adding / removing rows
        tableModel.fullWidth = true; // table is drawn using the full width if true
        tableModel.valuesTransformer = function(values) { return values }; // optional transformer
        tableModel.getValues = (function() {
            var values = [];
            for (var i of Object.keys(tableModel.rows)) {
                var row = tableModel.rows[i];
                var rowValues = {};
                for (var column of tableModel.columns) {
                    var $widget = row[column.label];
                    var value = this._getWidgetValue($widget);
                    rowValues[column.label] = value;
                }
                if (tableModel.rowExtraModels.length === tableModel.rows.length) {
                    rowValues.extraValues = tableModel.rowExtraModels[i].getValues();
                }
                values.push(rowValues);
            }
            return tableModel.valuesTransformer(values);
        }).bind(this);
        tableModel.addRow = function(rowData, rowType) {
            var rowWidgets = {};
            for (var column of tableModel.columns) {
                var rowBuilder = tableModel.rowBuilders[column.label];
                rowWidgets[column.label] = rowBuilder(rowData, rowType);
            }
            tableModel.rows.push(rowWidgets);
            if (tableModel.rowExtraBuilder) {
                tableModel.rowExtras.push(tableModel.rowExtraBuilder(rowData));
            }
            return rowWidgets;
        };
        return tableModel;
    }

    this._getWidgetValue = function($widget) {
        if ($widget.is("span")) {
            if($widget.attr('varname')) {
                return $widget.attr('varname');
            }
            return $widget.text();
        } else if ($widget.is("input") && $widget.attr("type") === "checkbox") {
            return $widget.is(":checked");
        } else {
            return $widget.val();
        }
    }

    this._getTable = function(tableModel, canRemoveFunction) {
        var $table = $("<table>", { class : "table borderless table-compact" });
        if (tableModel.fullWidth != true) {
            $table.css("width", "initial");
        }
        // head
        var $thead = $("<thead>");
        var $trHead = $("<tr>");
        if (tableModel.rowExtraBuilder) {
            $trHead.append($("<th>").css("width", "30px"));
        }
        for (var column of tableModel.columns) {
            var $th = $("<th>").css("vertical-align", "middle").text(column.label);
            if (column.width) {
                $th.css("width", column.width);
            }
            $trHead.append($th);
        }
        // add row button
        if (tableModel.dynamicRows) {
            var $addButton = $("<a>", { class : "btn btn-default" })
                        .append($("<span>", { class : "glyphicon glyphicon-plus" } ));
            if (this._instanceSettingsModel.mode === FormMode.VIEW) {
                $addButton.addClass("disabled");
            } else {
                $addButton.on("click", (function() {
                    var rowWidgets = tableModel.addRow({});
                    if (tableModel.rowExtraBuilder) {
                        var $extra = tableModel.rowExtras[tableModel.rowExtras.length-1];
                        this._addRow($tbody, tableModel, rowWidgets, $extra);
                    } else {
                        this._addRow($tbody, tableModel, rowWidgets);
                    }
                }).bind(this))
            }
            $trHead.append($("<th>").css("width", "80px").append($addButton));
        }
        $thead.append($trHead);
        $table.append($thead);
        // body
        var $tbody = $("<tbody>");
        // keys in reverse order because we are adding rows on top
        for (var i of Object.keys(tableModel.rows).reverse()) {
            var row = tableModel.rows[i];

            if (tableModel.rowExtraBuilder) {
                // add extra as row after actual row
                var $extra = tableModel.rowExtras[i];
                this._addRow($tbody, tableModel, row, $extra, canRemoveFunction);
            } else {
                this._addRow($tbody, tableModel, row, null, canRemoveFunction);
            }
        }
        $table.append($tbody);
        return $table
    }


    this._addRow = function($tbody, tableModel, tableModelRow, $extra, canRemoveFunction) {
        var $tr = $("<tr>");
        $tbody.prepend($tr);
        var $extraRow = null;
        var rowIndex = tableModel.rows.indexOf(tableModelRow);

        // add expand / collapse for extra
        if ($extra) {
            // create extra row
            var colspan = tableModel.columns.length + 1;
            if (tableModel.dynamicRows) {
                colspan++;
            }
            $extraRow = $("<tr>")
                .append($("<td>").css({"padding-left" : "50px", "padding-right" : "50px"}).attr("colspan", colspan)
                    .append($extra));
            // hiding / showing extra row
            $extraRow.hide();
            var $td = $("<td>");
            var $expandCollapse = $("<div>", { class : "glyphicon glyphicon-plus-sign" }).css("vertical-align", "middle");
            $expandCollapse.on("click", (function($extraRow, $expandCollapse) {
                $extraRow.toggle();
                if ($extraRow.is(":visible")) {
                    $expandCollapse.removeClass("glyphicon-plus-sign").addClass("glyphicon-minus-sign");
                } else {
                    $expandCollapse.removeClass("glyphicon-minus-sign").addClass("glyphicon-plus-sign");
                }
            }).bind(this, $extraRow, $expandCollapse));
            $tr.append($td);
            $td.append($expandCollapse);
        }

        for (var column of tableModel.columns) {
            var $td = $("<td>");
            $tr.append($td);
            var $widget = tableModelRow[column.label];
            $td.append($widget);
            // disbale widget if in view mode
            if (this._instanceSettingsModel.mode === FormMode.VIEW || (canRemoveFunction && !canRemoveFunction(tableModel.rows[rowIndex]))) {
                $widget.prop("disabled", true);
            }
        }
        // remove row button if in edit mode
        if (tableModel.dynamicRows) {
            $removeButton = $("<a>", { class : "btn btn-default" })
                        .append($("<span>", { class : "glyphicon glyphicon-minus" }));
            if (this._instanceSettingsModel.mode === FormMode.VIEW) {
                $removeButton.addClass("disabled");
            } else {
                if(!canRemoveFunction || canRemoveFunction(tableModel.rows[rowIndex])) {
                    $removeButton.on("click", function() {
                        $tr.remove();
                        if ($extraRow) {
                            $extraRow.remove();
                        }
                        var rowIndex = tableModel.rows.indexOf(tableModelRow);
                        tableModel.rows.splice(rowIndex, 1);
                        if (tableModel.rowExtraModels) {
                            tableModel.rowExtraModels.splice(rowIndex, 1);
                        }
                    });
                } else {
                    $removeButton.addClass("disabled");
                }
            }
            $tr.append($("<td>").append($removeButton));
        }
        // add extra row
        if ($extraRow) {
            $tbody.append($extraRow);
        }
    }


}