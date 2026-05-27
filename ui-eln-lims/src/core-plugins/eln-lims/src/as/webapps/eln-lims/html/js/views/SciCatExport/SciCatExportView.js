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

            var $entityTree = $("<legend>").append("Inclusion tree");
            $container.append($entityTree);

            var $infoBox1 = FormUtil.getInfoBox('Publication time constraint', [
                'Process of exporting data to SciCat is time consuming. After the resource has been exported you will receive email with detail link.',
            ]);
            $infoBox1.css('border', 'none');
            $container.append($infoBox1);

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

            var $formTitle = $('<h2>').append('SciCat Export Builder');
            $header.append($formTitle);

            var $exportButton = $('<input>', { 'type': 'submit', 'class': 'btn btn-primary', 'value': 'Initialize export',
                'onClick': '$("form[name=\'sciCatExportForm\']").submit()'});
            $header.append($exportButton);

            var sampleType = mainController.profile.getSampleTypeForSampleTypeCode("PUBLICATION");

            var propertyTypes = sampleType.propertyTypeGroups.flatMap(x => x.propertyTypes)

            this._paintPublicationProperties($container, propertyTypes);


            $container.append($('<br>'));
        };

    this._paintPublicationProperties = function($formColumn, propertyTypes) {
        var _this = this;
        var sampleTypeCode = "PUBLICATION";
        var sampleType = mainController.profile.getSampleTypeForSampleTypeCode(sampleTypeCode);

        var $fieldsetOwner = $('<div>');
        var $fieldsetRequired = $('<div>');

        var $fieldsetOptional = $('<div>');
        var $legendRequired = $('<legend>');
        var $legendOptional = $('<legend>');

        var requiredProperties = ["NAME", "PUBLICATION.DESCRIPTION", "PUBLICATION.ABSTRACT", "PUBLICATION.CREATOR", "PUBLICATION.PUBLISHER"];

        var $legend = null;
        var $fieldset = null;

        $fieldsetOwner.append($legendRequired).append($fieldsetRequired).append($legendOptional).append($fieldsetOptional);


        $legendRequired.text("Publication")
        $legendOptional.text("Optional publication parameters");



        var propertyGroupPropertiesOnForm = 0;
        for(var j = 0; j < propertyTypes.length; j++) {
            var propertyType = $.extend({}, propertyTypes[j]);

            if(requiredProperties.includes(propertyType.code)) {
                $legend = $legendRequired;
                $fieldset = $fieldsetRequired;
                propertyType.mandatory = true;
            } else {
                $legend = $legendOptional;
                $fieldset = $fieldsetOptional;
                propertyType.mandatory = false;
            }

            var propertyTypeV3 = profile.getPropertyTypeFromSampleTypeV3(this.exportModel.type, propertyType.code);
            var isMultiValue = false;
            if(propertyTypeV3.isMultiValue) {
                isMultiValue = propertyTypeV3.isMultiValue();
            }


            var semanticAnnotations = this._renderPropertyTypeSemanticAnnotations(propertyType.code);

            if(propertyType.code === profile.getInternalNamespacePrefix() + "ANNOTATIONS_STATE" || propertyType.code === "FREEFORM_TABLE_STATE" || propertyType.code === profile.getInternalNamespacePrefix() + "ORDER.ORDER_STATE" || propertyType.code === profile.getInternalNamespacePrefix() + "BARCODE" ) {
                continue;
            }

            var $controlGroup =  null;
            var value = null;

            var $component = FormUtil.getFieldForPropertyType(propertyType, value, isMultiValue);
            if(['SAMPLE', 'DATE', 'TIMESTAMP', "BOOLEAN", "CONTROLLEDVOCABULARY"].includes(propertyType.dataType)) {
                _refreshableFields.push($component);
            }

            $component.val(value);

            var changeEvent = function(propertyType, isMultiValueProperty) {
                return function(jsEvent, newValue) {
                    var propertyTypeCode = null;
                    propertyTypeCode = propertyType.code;
                    var field = $(this);
                    if(propertyType.dataType === "BOOLEAN") {
                        _this.exportModel.properties[propertyTypeCode] = FormUtil.getBooleanValue(field);
                    } else if (propertyType.dataType === "TIMESTAMP" || propertyType.dataType === "DATE") {
                        if (jsEvent.date === false) {
                            _this.exportModel.properties[propertyTypeCode] = "";
                        } else {
                            var timeValue = $($(field.children()[0]).children()[0]).val();
                            var isValidValue = Util.isDateValid(timeValue, propertyType.dataType === "DATE");
                            if(!isValidValue.isValid) {
                                Util.showUserError(isValidValue.error);
                            } else {
                                _this.exportModel.properties[propertyTypeCode] = timeValue;
                            }
                        }
                    } else {
                        if(newValue !== undefined && newValue !== null) {
                            _this.exportModel.properties[propertyTypeCode] = Util.getEmptyIfNull(newValue);
                        } else {
                            var lastSelected = Util.getEmptyIfNull($('option', this).filter(':selected:last').val());
                            var dataLast = field.data('last');
                            if(propertyType.dataType === "CONTROLLEDVOCABULARY" && isMultiValueProperty) {
                                var props = _this.exportModel.properties[propertyTypeCode];
                                if (field.val()) {
                                    if(props !== undefined) {
                                        if(props != '' && field.val().includes('')) {
                                            _this.exportModel.properties[propertyTypeCode] = '';
                                            field.val([]);
                                        } else {
                                            if(props == '' && field.val().includes('')) {
                                                var removedEmpty = field.val().filter(x => x != '');
                                                _this.exportModel.properties[propertyTypeCode] = removedEmpty;
                                                field.val(removedEmpty);
                                            } else {
                                                _this.exportModel.properties[propertyTypeCode] = Util.getEmptyIfNull(field.val());
                                            }
                                        }
                                    } else {
                                        if(field.val().includes('')) {
                                            if(dataLast == undefined) {
                                                var val = field.val().filter(x => x != '');
                                                _this.exportModel.properties[propertyTypeCode] = val;
                                                field.val(val);
                                            } else {
                                                _this.exportModel.properties[propertyTypeCode] = '';
                                                field.val([]);
                                            }
                                        } else {
                                            _this.exportModel.properties[propertyTypeCode] = field.val();
                                        }
                                    }
                                } else {
                                    _this.exportModel.properties[propertyTypeCode] = Util.getEmptyIfNull(field.val());
                                }
                            } else {
                                var value = Util.getEmptyIfNull(field.val());
                                _this.exportModel.properties[propertyTypeCode] = value;
                            }
                            field.data('last', field.val());
                        }
                    }
                }
            }

            if(propertyType.dataType === "TIMESTAMP" || propertyType.dataType === "DATE") {
                $("body").on("dp.change", "#"+FormUtil.escapeIdForSelectors($component.attr("id")), changeEvent(propertyType));
            } else {
                $("body").on("change", "#"+FormUtil.escapeIdForSelectors($component.attr("id")), changeEvent(propertyType, isMultiValue));
            }

            $controlGroup = FormUtil.getFieldForComponentWithLabel($component, propertyType.label, null, null, semanticAnnotations);

            $fieldset.append($controlGroup);

            if(propertyType.code !== profile.getInternalNamespacePrefix() + "ANNOTATIONS_STATE") {
                propertyGroupPropertiesOnForm++;
            }
        }

        if(propertyGroupPropertiesOnForm === 0) {
            $legendRequired.remove();
            $legendOptional.remove();
        }

        $legendRequired.prepend(FormUtil.getShowHideButton($fieldsetRequired, "SCICAT_EXPORT" + "-REQUIRED-" + _this._viewId));
        var showHideOptional = FormUtil.getShowHideButton($fieldsetOptional, "SCICAT_EXPORT" + "-OPTIONAL-" + _this._viewId, true);
        $legendOptional.prepend(showHideOptional);
        $formColumn.append($fieldsetOwner);

        return false;
    }


    this._renderPropertyTypeSemanticAnnotations = function(propertyTypeCode) {
        var annotations = this._getAllSemanticAnnotations(propertyTypeCode);
        if (annotations && annotations.length > 0) {
            var $group = $("<div>", {class : "form-group"});
            $group.append($("<label>", {class : "control-label"}).text("Semantic Annotations:"));
            var $lines = $("<div>", {class : "controls" });
            var _this = this;
            annotations.forEach(function(annotation) {
                $lines.append(_this._renderSemanticAnnotation(annotation.getPredicateAccessionId(),
                    annotation.getPredicateOntologyId(),
                    annotation.getPredicateOntologyVersion()));
            });
            $group.append($lines);
            return $group;
        }
        return null;
    }

    this._getAllSemanticAnnotations = function(propertyTypeCode) {
        // Using a dict because the same property type annotations appear for the assignments if not
        // overloaded
        var semanticAnnotations = {};
        var propertyAssignment = this._getPropertyAssignment(propertyTypeCode);
        if (propertyAssignment) {
            [propertyAssignment.semanticAnnotations, propertyAssignment.propertyType.semanticAnnotations].forEach(function(annotations) {
                if (annotations) {
                    annotations.forEach(function(annotation) {
                        semanticAnnotations[annotation.permId.permId] = annotation;
                    });
                }
            });
        }
        return Object.values(semanticAnnotations);
    }

    this._getPropertyAssignment = function(propertyTypeCode) {
        if (this.exportModel.type && this.exportModel.type.propertyAssignments) {
            var propertyAssignments = this.exportModel.type.propertyAssignments;
            for (var i = 0; i < propertyAssignments.length; i++) {
                var propertyAssignment = propertyAssignments[i];
                if (propertyAssignment.propertyType.code === propertyTypeCode) {
                    return propertyAssignment;
                }
            }
        }
        return null;
    }

    this._renderSemanticAnnotation = function(accessionId, ontologyId, ontologyVersion) {
        var $line = $("<div>");
        $line.append(this._asHyperLinkOrText(accessionId));
        $line.append(" (Ontology: ");
        $line.append(this._asHyperLinkOrText(ontologyId));
        if (ontologyVersion && ontologyVersion.length > 0) {
            $line.append(", Version: ");
            $line.append(this._asHyperLinkOrText(ontologyVersion));
        }
        $line.append(")");
        return $line;
    }

    this._asHyperLinkOrText = function(text) {
        return (text && text.startsWith("http")) ? FormUtil.asHyperlink(text) : text;
    }


}