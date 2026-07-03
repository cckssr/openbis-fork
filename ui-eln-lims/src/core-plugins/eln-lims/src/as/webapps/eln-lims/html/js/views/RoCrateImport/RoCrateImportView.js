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
function RoCrateImportView(importController, importModel) {
    var _refreshableFields = [];
    var _this = this;

    this.importModel = importModel;
    this.importController = importController;
    this._viewId = mainController.getNextId();


    this.refresh = function() {
        for(var field of _refreshableFields) {
            field.refresh();
        }
    }

    this.repaint = function(views) {
        var _this = this;
        var $header = views.header;
        var $container = views.content;

        var $form = $("<div>");
        var $formColumn = $("<form>", {
            'name': 'roCrateImportForm',
            'role': 'form',
            'action': 'javascript:void(0);',
            'onsubmit': 'mainController.currentView.importSelected();'
        });
        $form.append($formColumn);

        var $formTitle = $('<h2>').append('RO-Crate Import Builder');
        $header.append($formTitle);

        var $importButton = $('<input>', { 'type': 'submit', 'class': 'btn btn-primary', 'value': 'Initialize import',
            'onClick': '$("form[name=\'roCrateImportForm\']").submit()'});
        $header.append($importButton);


        var component = $("<div>");
        component.append($('<br>'));

        var modeLabel = FormUtil.createLabel('Select import mode');
        component.append(modeLabel);
        var $modeDropdown = $("<select>", { 'id': 'importModeDropdown-roCrate' });
        $modeDropdown
            .append($("<option>", { 'value':'fail', 'text':"Fail if exists"}))
            .append($("<option>", { 'value':'ignore', 'text':"Ignore if exists"}))
            .append($("<option>", { 'value':'update', 'text':"Update if exists", 'selected':'selected'}));
        Select2Manager.add($modeDropdown);
        $modeDropdown.refresh = function() {
            Select2Manager.add($(this));
        }
        _refreshableFields.push($modeDropdown);

        component.append($modeDropdown);

        var fileChooser = $('<input>', { 'type': 'file', 'id': 'fileToRegister', 'required': '', 'accept': 'application/json,application/zip' });
        var fileNameDisplay = $('<span>', { 'id': 'fileNameDisplay' });
        var fileChooserButton = $('<a>', { 'type': 'button', 'class': 'btn btn-default' }).css("margin-left", "4px").text('Choose file...');

        fileNameDisplay.css({
            "vertical-align": "middle",
            "margin-left": "8px",
            "font-size": "15px",
        });

        fileChooser.css('display', 'none'); // hide native input

        var fileChooserBtnClick = function() {
            fileChooser.trigger('click');
        };
        fileChooserButton.click(fileChooserBtnClick);
        fileChooserButton.refresh = function() {
            $(this).off('click');
            $(this).on('click', fileChooserBtnClick);
        }
        _refreshableFields.push(fileChooserButton);

        var fileChooserChange = function(event) {
            var selectedFile = fileChooser[0].files[0];
            _this.importModel.file = selectedFile;
            fileNameDisplay.text(selectedFile ? selectedFile.name : '');
            fileNameDisplay.attr('title', selectedFile ? selectedFile.name : ''); // tooltip for full name on hover
        }
        fileChooser.change(fileChooserChange);
        fileChooser.refresh = function() {
            $(this).off('change');
            $(this).on('change', fileChooserChange);
        }
        _refreshableFields.push(fileChooser);

        var componentChooser = $("<div>");
        componentChooser.append(fileChooser).append(fileChooserButton).append(fileNameDisplay);

        var fileChooserBoxGroup = FormUtil.getFieldForComponentWithLabel(componentChooser, 'Select RO-Crate file to import');
        component.append(fileChooserBoxGroup);
        component.append($('<br>'));


        var label = FormUtil.createLabel('Select fallback project to import entities');
        var $infoBox1 = FormUtil.getInfoBox('Import destination logic', [
            'The crate\'s own openBIS references take priority for placement; the selected project is only used as a fallback for data that doesn\'t already belong somewhere.',
        ]);
        $infoBox1.css('border', 'none');

        component.append($infoBox1);
        component.append(label);


        var advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(false, true, "Search for Project",
            false, false, false, true, false);

        _this.importModel.searchDropdown = advancedEntitySearchDropdown;

        advancedEntitySearchDropdown.onChange(function(selected) {
            _this.importModel.entity = selected[0];
        });
        advancedEntitySearchDropdown.onUnselect(function(selected) {
            _this.importModel.entity = null;
        });

        advancedEntitySearchDropdown.init(component);
        _refreshableFields.push(advancedEntitySearchDropdown);

        $form.append(component);

        $container.append($form);


        $container.append($('<br>'));
    };


}