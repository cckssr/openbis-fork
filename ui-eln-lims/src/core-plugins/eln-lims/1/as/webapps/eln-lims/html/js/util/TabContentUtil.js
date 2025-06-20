/*
 * Copyright 2025 ETH Zuerich, Scientific IT Services
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

/**
 * Utility class IconUtil, created as anonymous, it contains utility methods mainly to show icons.
 *
 * Contains methods used getting icons for different ELN controls.
 */
var TabContentUtil = new function() {

    this._counter = 1000;

    this.getTopLevelTabInfo = function(name) {
        var iconType = IconUtil.getNavigationIcon(name)
        var icon = IconUtil.getIcon(iconType);

        return {
            label: name,
            changed: false,
            id: name + '-id',
            icon: icon[0].outerHTML,
        }

    }

    this.getSpaceTabInfo = function(space, mode) {
            var iconType = IconUtil.getNavigationIcon('SPACE', {
                        isHomeSpace: false
                    });
            var icon = IconUtil.getIcon(iconType);

            return {
                label: space,
                changed: mode === FormMode.EDIT,
                id: space + '-id',
                icon: icon[0].outerHTML,
            }

        }

    this.getDataSetTabInfo = function(dataset, mode, optionalPrefix) {

        if(mode === FormMode.CREATE) {
            return this.getCleanTab("CREATE_DATASET", true, 'create-dataset-id')
        }

         var name = dataset.properties[profile.getInternalNamespacePrefix() + 'NAME'];
         name = name ? name : dataset.code;
         var id = dataset.code;
         if(optionalPrefix) {
             name = optionalPrefix + ": " + name
             id = optionalPrefix + "-" + id
         }

         var iconType = IconUtil.getNavigationIcon('DATASET');
         var icon = IconUtil.getIcon(iconType);

         return {
             label: name,
             changed: mode === FormMode.EDIT,
             id: id,
             icon: icon[0].outerHTML,
         }
     }

    this.getSampleTabInfo = function(sample, mode, optionalPrefix) {
        var name = sample.properties[profile.getInternalNamespacePrefix() + 'NAME'];
        name = name ? name : sample.code;
        var id = sample.permId;
        if(optionalPrefix) {
            name = optionalPrefix + ": " + name
            id = optionalPrefix + "-" + id
        }

        var iconType = IconUtil.getNavigationIcon('SAMPLE', {
            sampleTypeCode: sample.sampleTypeCode
        });
        var icon = IconUtil.getIcon(iconType);

        return {
            label: name,
            changed: mode === FormMode.EDIT,
            id: id,
            icon: icon[0].outerHTML,
        }
    }

    this.getExperimentTabInfo = function(experiment, mode, optionalPrefix) {

        if(mode === FormMode.CREATE) {
            return this.getCleanTab("CREATE_EXPERIMENT", true, 'create-collection-' + mainController.getNextId())
        }

        var name = experiment.properties[profile.getInternalNamespacePrefix() + 'NAME'];
        name = name ? name : experiment.code;
        var id = experiment.permId;

        if(optionalPrefix) {
            name = optionalPrefix + ": " + name
            id = optionalPrefix + "-" + id
        }

        var iconType = IconUtil.getNavigationIcon('EXPERIMENT', {
            isExperimentWithoutChildren: !this._isExperimentWithChildren(experiment)
        });
        var icon = IconUtil.getIcon(iconType);

        return {
            label: name,
            changed: mode === FormMode.EDIT,
            id: id,
            icon: icon[0].outerHTML,
        }
    }

    this._isExperimentWithChildren = function(experiment) {
         var experimentIdentifier = null;
         if(experiment.getIdentifier) {
            experimentIdentifier = experiment.getIdentifier().getIdentifier();
         } else {
            experimentIdentifier = experiment.identifier;
         }


         var experimentSpaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(experimentIdentifier)
         var isInventorySpace = profile.isInventorySpace(experimentSpaceCode)
         var isFormView = experiment.properties &&
                             experiment.properties["DEFAULT_COLLECTION_VIEW"] &&
                             experiment.properties["DEFAULT_COLLECTION_VIEW"] === "FORM_VIEW"

         var isExperimentWithChildren = isFormView
         var experimentTypeCode = null;
         if(experiment.getType) {
            experimentTypeCode = experiment.getType().getCode();
         } else {
            experimentTypeCode = experiment.experimentTypeCode;
         }

         var isExperimentWithNoChildren = experimentTypeCode === "COLLECTION" || isInventorySpace
         return isExperimentWithChildren || !isExperimentWithNoChildren
    }

    this.getProjectTabInfo = function(project, mode, optionalPrefix) {
        var name = project.code;
        var id = project.permId;
        if(optionalPrefix) {
            name = optionalPrefix + ": " + name
            id = optionalPrefix + "-" + id
        }

        var iconType = IconUtil.getNavigationIcon('PROJECT');
        var icon = IconUtil.getIcon(iconType);

        return {
            label: name,
            changed: mode === FormMode.EDIT,
            id: id,
            icon: icon[0].outerHTML,
        }
    }

    this.getCleanTab = function(name, changed, uniqueId) {
        var label = name;
        var id = uniqueId;

        if(!label) {
            label = "DUMMY"
        }

        if(!id) {
            id = 'dummy-tab-id-' + this._counter++;
        }

        return {
            label: label,
            changed: changed === true,
            id: id,
        }
    }


    this.getToolTabInfo = function(tabName, optionalText) {
        var iconType = IconUtil.getNavigationIcon(tabName)
        var icon = IconUtil.getIcon(iconType);

        var label = tabName
        if(optionalText) {
            if(optionalText.length > 5) {
                label = label + ": " + optionalText.substr(0, 5) + "\u2026";
            } else {
                label = label + ": " + optionalText;
            }
        }

        return {
            label: tabName,
            changed: false,
            id: tabName + '-id',
            icon: icon[0].outerHTML,
            page: 'tools',
        }
    }

}