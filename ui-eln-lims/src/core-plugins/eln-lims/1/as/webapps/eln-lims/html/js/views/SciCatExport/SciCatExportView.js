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
function SciCatExportView(exportController, exportModel) {
    var _refreshableFields = [];
    var _this = this;

    this.exportModel = exportModel;
    this.exportController = exportController;
    this._viewId = mainController.getNextId();


    this.refresh = function() {
        for(var field of _refreshableFields) {
            field.refresh();
        }
    }

    this.repaint = function(views) {
            var $header = views.header;
            var $container = views.content;

            var $form = $("<div>");
            var $formColumn = $("<form>", {
                'name': 'sciCatExportForm',
                'role': 'form',
                'action': 'javascript:void(0);',
                'onsubmit': 'mainController.currentView.exportSelected();'
            });
            $form.append($formColumn);


            var $options = $("<div>");
            var $compatible = $("<span class='checkbox'><label><input type='checkbox' checked id='COMPATIBLE-IMPORT-"+_this._viewId+"' disabled>Make import compatible</label></span>");
            $options.append($compatible);

            $options.append($("<legend>").append("File formats"));

            var $pdf = $("<span class='checkbox'><label><input type='checkbox' checked id='PDF-EXPORT-"+_this._viewId+"'>Export metadata as PDF</label></span>");
            $options.append($pdf);
            var $xlsx = $("<span class='checkbox'><label><input type='checkbox' checked id='XLSX-EXPORT-"+_this._viewId+"'>Export metadata as XLSX</label></span>");
            $options.append($xlsx);
            var $data = $("<span class='checkbox'><label><input type='checkbox' checked id='DATASET-EXPORT-"+_this._viewId+"'>Export dataset data</label></span>");
            $options.append($data);
            var $data = $("<span class='checkbox'><label><input type='checkbox' checked id='FILES-EXPORT-"+_this._viewId+"'>Export files</label></span>");
            $options.append($data);


            var $hierarchyInclusions = $("<legend>").append("Hierarchy Inclusions (Same Space)");

            $options.append($hierarchyInclusions);

            var $withLevelsAbove = $("<span class='checkbox'><label><input type='checkbox' id='LEVELS-ABOVE-EXPORT' checked disabled>Include levels above (Always included)</label></span>");
            $options.append($withLevelsAbove);
            var $includeParents = $("</span><span class='checkbox'><label><input type='checkbox' id='PARENTS-EXPORT-"+_this._viewId+"'>Include Object and Dataset parents</label></span>").css({ "padding-left" : "20px" });
            $options.append($includeParents);

            var $levelsBelow = $("<span class='checkbox'><label><input type='checkbox' id='LEVELS-BELOW-EXPORT-"+_this._viewId+"'>Include levels below</label></span>");
            $options.append($levelsBelow);
            var $includeChildren = $("<span class='checkbox'><label><input type='checkbox' id='CHILDREN-EXPORT-"+_this._viewId+"' disabled>Include Object and Dataset children</label></span>").css({ "padding-left" : "20px" });
            $options.append($includeChildren);

            var levelsBelowChange = function(event) {
                $("#CHILDREN-EXPORT-"+_this._viewId)[0].disabled = !event.target.checked;
                if (!event.target.checked) {
                    $("#CHILDREN-EXPORT-"+_this._viewId)[0].checked = false;
                    var enabled = $("#PARENTS-EXPORT-"+_this._viewId)[0].checked || $("#CHILDREN-EXPORT-"+_this._viewId)[0].checked;
                    if(!enabled) {
                        $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].disabled = true;
                        $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].checked = false;
                    }
                }
            }
            $levelsBelow.change(levelsBelowChange);
            $levelsBelow.refresh = function() {
                this.off('change')
                this.change(levelsBelowChange);
            }
            _refreshableFields.push($levelsBelow)

            var includeParentsChildrenChangeEvent = function(event) {
                var enabled = $("#PARENTS-EXPORT-"+_this._viewId)[0].checked || $("#CHILDREN-EXPORT-"+_this._viewId)[0].checked;
                $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].disabled = !enabled;
                if (!enabled) {
                    $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].checked = false;
                }
            }
            $includeParents.change(includeParentsChildrenChangeEvent);
            $includeParents.refresh = function() {
                this.off('change')
                this.change(includeParentsChildrenChangeEvent);
            }
            _refreshableFields.push($includeParents)
            $includeChildren.change(includeParentsChildrenChangeEvent);
            $includeChildren.refresh = function() {
                this.off('change')
                this.change(includeParentsChildrenChangeEvent);
            }
            _refreshableFields.push($includeChildren)



            var $spaceInclusions = $("<legend>").append("Hierarchy Inclusions (Other Spaces)");
            $options.append($spaceInclusions);
            var $includeOtherSpaces = $("<span class='checkbox'><label><input type='checkbox' id='OTHER-SPACES-EXPORT-"+_this._viewId+"' disabled>Include Objects and Datasets parents and children</label></span>");
            $options.append($includeOtherSpaces);


    		$container.append($options);


            var $tree = $('<div>', { 'id' : 'exportsTree-'+_this._viewId });
            $formColumn.append(FormUtil.getBox().append($tree));
//            $form.css('width', '89%');

            var $entityTree = $("<legend>").append("Inclusion tree");
            $container.append($entityTree);

            $container.append($form);

            this.exportModel.tree = TreeUtil.getCompleteTree($tree);
            this.exportModel.tree.fancytree('getTree').rootNode.children[0].setExpanded();

            this.exportModel.tableModel = ExportUtil.getTableModel();

            $formColumn.refresh = function() {
                $("form[name='sciCatExportForm']").children().remove()
                var $tree = $('<div>', { 'id' : 'exportsTree-'+_this._viewId });
                $("form[name='sciCatExportForm']").append(FormUtil.getBox().append($tree));
                _this.exportModel.tree = TreeUtil.getCompleteTree($tree);
                _this.exportModel.tree.fancytree('getTree').rootNode.children[0].setExpanded();
            }
            _refreshableFields.push($formColumn);

            var $formTitle = $('<h2>').append('Sci Cat Export Builder');
            $header.append($formTitle);

            var $exportButton = $('<input>', { 'type': 'submit', 'class': 'btn btn-primary', 'value': 'Initialize export',
                'onClick': '$("form[name=\'sciCatExportForm\']").submit()'});
            $header.append($exportButton);


            $container.append($("<legend>").append("Sci Cat Metadata"));
            this.paintDerivedBox($container);
            this.paintScientificMetadata($container)


            $container.append($('<br>'));
        };

        this.paintDerivedBox = function ($container) {
            this.$derivedBox = FormUtil.getTextInputField('sci-cat-derived', 'SciCat identifier', false);
            var derivedFormGroup = FormUtil.getFieldForComponentWithLabel(this.$derivedBox, 'Derived', null, true);
            derivedFormGroup.css('width', '89%');
            $container.append(derivedFormGroup);
        };

        this.paintScientificMetadata = function ($container) {

            var tableModel = this._getScientificMetadataTableModel();
            this.tableModel = tableModel;
            this.table = this._getTable(tableModel);
//            this.table.css( { "margin-left" : "30px" } );

            var scientificMetadataFormGroup = FormUtil.getFieldForComponentWithLabel(this.table, 'Scientific Metadata', null, true);
            scientificMetadataFormGroup.css('width', '90%');
            $container.append(scientificMetadataFormGroup);
        };

        this._getScientificMetadataTableModel = function() {
            var tableModel = this._getTableModel();
            tableModel.dynamicRows = true;
            // define columns
            tableModel.columns = [{ label : "Key" }, { label : "Value" }];
            tableModel.rowBuilders = {
                "Key" : function(rowData) {
                    return $("<input>", { type : "text", class : "form-control", placeholder : 'Key' }).val(rowData.key);
                },
                "Value" :  function(rowData) {
                     return $("<input>", { type : "text", class : "form-control", placeholder : 'Value' }).val(rowData.value);
                 },
            };
            // transform output
            tableModel.valuesTransformer = function(values) {
                return values.map(function(value) {
                    return {
                        key : value["Key"],
                        value : value["Value"],
                    };
                });
            }
            return tableModel;
        }

        this._getWidgetValue = function($widget) {
        		if ($widget.is("span")) {
        			return $widget.text();
        		} else if ($widget.is("input") && $widget.attr("type") === "checkbox") {
        			return $widget.is(":checked");
        		} else {
        			return $widget.val();
        		}
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
            tableModel.addRow = function(rowData) {
                var rowWidgets = {};
                for (var column of tableModel.columns) {
                    var rowBuilder = tableModel.rowBuilders[column.label];
                    rowWidgets[column.label] = rowBuilder(rowData);
                }
                tableModel.rows.push(rowWidgets);
                if (tableModel.rowExtraBuilder) {
                    tableModel.rowExtras.push(tableModel.rowExtraBuilder(rowData));
                }
                return rowWidgets;
            };
            return tableModel;
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
                var _this = this;
                var $addButton = FormUtil.getToolbarButton("PLUS");
                $addButton.children().css("font-size", '22px')
                    $addButton.on("click", (function() {
                        var rowWidgets = tableModel.addRow({});
                        if (tableModel.rowExtraBuilder) {
                            var $extra = tableModel.rowExtras[tableModel.rowExtras.length-1];
                            _this._addRow($tbody, tableModel, rowWidgets, $extra);
                        } else {
                            _this._addRow($tbody, tableModel, rowWidgets);
                        }
                    }).bind(_this))
                $addButton.refresh = function() {
                    this.off("click");
                    this.on("click", (function() {
                        var rowWidgets = tableModel.addRow({});
                        if (tableModel.rowExtraBuilder) {
                            var $extra = tableModel.rowExtras[tableModel.rowExtras.length-1];
                            _this._addRow($tbody, tableModel, rowWidgets, $extra);
                        } else {
                            _this._addRow($tbody, tableModel, rowWidgets);
                        }
                    }).bind(_this))
                }
                _refreshableFields.push($addButton);
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
                var _this = this;
                $expandCollapse.refresh = function() {
                    this.off("click");
                    this.on("click", (function($extraRow, $expandCollapse) {
                        $extraRow.toggle();
                        if ($extraRow.is(":visible")) {
                            $expandCollapse.removeClass("glyphicon-plus-sign").addClass("glyphicon-minus-sign");
                        } else {
                            $expandCollapse.removeClass("glyphicon-minus-sign").addClass("glyphicon-plus-sign");
                        }
                    }).bind(_this, $extraRow, $expandCollapse))
                }
                _refreshableFields.push($expandCollapse);

                $tr.append($td);
                $td.append($expandCollapse);
            }

            for (var column of tableModel.columns) {
                var $td = $("<td>");
                $tr.append($td);
                var $widget = tableModelRow[column.label];
                $td.append($widget);

            }
            // remove row button if in edit mode
            if (tableModel.dynamicRows) {
                $removeButton = FormUtil.getToolbarButton("MINUS");
                $removeButton.children().css("font-size", '22px')
                    if(!canRemoveFunction || canRemoveFunction(tableModel.rows[rowIndex])) {
                        var _this = this;
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
                        $removeButton.refresh = function() {
                            this.off("click");
                            this.on("click", (function() {
                                $tr.remove();
                                if ($extraRow) {
                                    $extraRow.remove();
                                }
                                var rowIndex = tableModel.rows.indexOf(tableModelRow);
                                tableModel.rows.splice(rowIndex, 1);
                                if (tableModel.rowExtraModels) {
                                    tableModel.rowExtraModels.splice(rowIndex, 1);
                                }
                            }))
                        }
                        _refreshableFields.push($removeButton);
                    } else {
                        $removeButton.addClass("disabled");
                    }
                $tr.append($("<td>").append($removeButton));
            }
            // add extra row
            if ($extraRow) {
                $tbody.append($extraRow);
            }
        }


}