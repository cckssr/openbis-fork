/*
 * Copyright 2011 ETH Zuerich, CISD
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

function ZenodoExportView(exportController, exportModel) {
    var _refreshableFields = [];
    var _this = this;
    var exportModel = exportModel;

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
            'name': 'zenodoExportForm',
            'role': 'form',
            'action': 'javascript:void(0);',
            'onsubmit': 'mainController.currentView.exportSelected();'
        });
        $form.append($formColumn);

        var $options = $("<div>");
        var $compatible = $("<span class='checkbox'><label><input type='checkbox' checked id='COMPATIBLE-IMPORT-"+_this._viewId+"'>Make import compatible</label></span>");
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

        var $withLevelsAbove = $("<span class='checkbox'><label><input type='checkbox' id='LEVELS-ABOVE-EXPORT-"+_this._viewId+"' checked>Include levels above</label></span>");
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

        var $entityTree = $("<legend>").append("Inclusion tree");
        $container.append($entityTree);

        var $infoBox1 = FormUtil.getInfoBox('Publication time constraint', [
            'After the resource has been exported it should be published in Zenodo UI within 2 hours.',
            'Otherwise, the publication metadata will not be registered in openBIS.'
        ]);
        $infoBox1.css('border', 'none');
        $container.append($infoBox1);

        var $tree = $('<div>', { 'id' : 'exportsTree' });
        $formColumn.append(FormUtil.getBox().append($tree));

        $formColumn.refresh = function() {
            $("form[name='zenodoExportForm']").children().remove()
            var $tree = $('<div>', { 'id' : 'exportsTree-'+_this._viewId });
            $("form[name='zenodoExportForm']").append(FormUtil.getBox().append($tree));
            exportModel.tree = TreeUtil.getCompleteTree($tree);
            exportModel.tree.fancytree('getTree').rootNode.children[0].setExpanded();
        }
        _refreshableFields.push($formColumn);

        $container.append($form);

        exportModel.tree = TreeUtil.getCompleteTree($tree);
        exportModel.tree.fancytree('getTree').rootNode.children[0].setExpanded();
        exportModel.tableModel = ExportUtil.getTableModel();

        this.paintTitleTextBox($container);
        this.paintFileNameTextBox($container);
        if (exportModel.tableModel.getValues().length > 0) {
            ExportUtil.paintGroupCheckboxes($container, "zenodo-groups");
        }

        var $formTitle = $('<h2>').append('Zenodo Export Builder');
        $header.append($formTitle);

        var $exportButton = $('<input>', { 'type': 'submit', 'class': 'btn btn-primary', 'value': 'Export Selected',
            'onClick': '$("form[name=\'zenodoExportForm\']").submit()'});
        $header.append($exportButton);

        $container.append($('<br>'));
    };

    this.paintTitleTextBox = function ($container) {
        this.$titleTextBox = FormUtil.getTextInputField('zenodo-submission-title', 'Submission title', true);
        var titleTextBoxFormGroup = FormUtil.getFieldForComponentWithLabel(this.$titleTextBox, 'Submission Title', null, true);
        titleTextBoxFormGroup.css('width', '50%');
        $container.append(titleTextBoxFormGroup);
    };

    this.paintFileNameTextBox = function ($container) {
        this.$fileNameBox = FormUtil.getTextInputField('zenodo-submission-file-name', 'Submission file name', false);
        var fileNameBoxFormGroup = FormUtil.getFieldForComponentWithLabel(this.$fileNameBox, 'Submission file name', null, true);
        fileNameBoxFormGroup.css('width', '50%');
        $container.append(fileNameBoxFormGroup);
    };



}