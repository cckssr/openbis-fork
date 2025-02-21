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
var IconUtil = new function() {

    this.getNavigationIcon = function(type, optionalParameters) {
        var icon = {
            type: null,
            url: null,
            class: null,
            text: null
        }
        //TODO add custom icons logic here ?

        if(typeof type === "object") {
            if(type.icon) {
                icon.class = type.icon;
            }
        } else if(type === "LAB_NOTEBOOK") {
            icon.class = "glyphicon glyphicon-book";
        } else if(type === "INVENTORY") {
            icon.class = "fa fa-cubes";
        } else if(type === "STOCK") {
            icon.class = "fa fa-shopping-cart";
        } else if(type === "STOCK") {
            icon.class = "fa fa-shopping-cart";
        } else if(type === "UTILITIES") {
            icon.class = "glyphicon glyphicon-wrench";
        } else if(type === "EXPORTS") {
            icon.class = "glyphicon glyphicon-export";
        } else if(type === "STORAGE_MANAGER") {
            icon.class = "glyphicon glyphicon-file";
        } else if(type === "USER_PROFILE") {
            icon.class = "glyphicon glyphicon-user";
        } else if(type === "GENERATE_BARCODES") {
            icon.class = "glyphicon glyphicon-barcode";
        } else if(type === "SAMPLE_BROWSER") {
            icon.class = "glyphicon glyphicon-list-alt";
        } else if(type === "VOCABULARY_BROWSER") {
            icon.class = "glyphicon glyphicon-list-alt";
        } else if(type === "ADVANCED_SEARCH") {
            icon.class = "glyphicon glyphicon-search";
        } else if(type === "DROPBOX_MONITOR") {
            icon.class = "glyphicon glyphicon-info-sign";
        } else if(type === "ARCHIVING_HELPER") {
            icon.class = "fancytree-icon";
            icon.url = "./img/archive-not-requested-icon.png";
        } else if(type === "UNARCHIVING_HELPER") {
            icon.class = "glyphicon glyphicon-open";
        } else if(type === "CUSTOM_IMPORT") {
            icon.class = "glyphicon glyphicon-import";
        } else if(type === "USER_MANAGER") {
            icon.class = "fa fa-users";
        } else if(type === "USER_MANAGEMENT_CONFIG") {
            icon.class = "fa fa-users";
        } else if(type === "TRASHCAN") {
            icon.class = "glyphicon glyphicon-trash";
        } else if(type === "SETTINGS") {
            icon.class = "glyphicon glyphicon-cog";
        } else if(type === "OTHERTOOLS") {
            icon.class = "glyphicon glyphicon-wrench";
        } else if(type === "EXPORT_TO_ZIP") {
            icon.class = "glyphicon glyphicon-export";
        } else if(type === "EXPORT_TO_RESEARCH_COLLECTION") {
            icon.class = "fancytree-icon";
            icon.url = "./img/research-collection-icon.png";
        } else if(type === "EXPORT_TO_ZENODO") {
            icon.class = "glyphicon glyphicon-export";
        } else if(type === "ABOUT") {
            icon.class = "glyphicon glyphicon-info-sign";
        } else if(type === "DATASET") {
            icon.class = "fa fa-database";
        } else if(type === "SPACE") {
            if(optionalParameters && optionalParameters.isHomeSpace) {
                icon.class = "material-icons";
                icon.text = "folder_shared";
            }
//            else {
//                icon.class = "material-icons";
//                icon.text = "key";
//            }
        } else if(type === "SAMPLE") {
            if(optionalParameters && optionalParameters.sampleTypeCode) {
                var sampleTypeCode = optionalParameters.sampleTypeCode;
                if(sampleTypeCode.indexOf("EXPERIMENT") > -1) {
                    icon.class = "fa fa-flask";
                } else if(sampleTypeCode === "ENTRY") {
                    icon.class = "fa fa-file-text";
                } else {
                    icon.class = "fa fa-file";
                }
            }
        } else if(type === "EXPERIMENT") {
            icon.class = "fa fa-table";
        }
//          else if(type === "PROJECT") {
//            icon.class = "material-icons";
//            icon.text = "rule_folder";
//        }

        return icon;
    };



}