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

    this.getIcon = function(icon, size) {
        var $icon = null
        if(!size) {
            size = 18;
        }

        switch(icon.type) {
            case "img":
            case "img_with_class":
                $icon = $("<img/>").attr("src", icon.url);
                $icon.css("width", size+"px");
                $icon.css("height", size+"px");
                break;
            default:
                $icon = $("<span/>");
                break;
        }

        if(icon.type === "img_with_class") {
            $icon.addClass(icon.class)
        } else if(icon.type === "font") {
            if (icon.class) {
                $icon.addClass(icon.class)
            }

            if(icon.text) {
                $icon.text(icon.text);
                $icon.css("font-size", size+"px");
            }
        } else if(Array.isArray(icon.type)) {
            for (let i = 0; i < icon.type.length; i++) {
                var type = icon.type[i];
                var tempIcon;
                if(type === "font") {
                    tempIcon = $("<span/>").css("vertical-align", "middle");
                    if(icon.class[i]) {
                        tempIcon.addClass(icon.class[i]);
                    }
                    if(icon.text[i]) {
                        tempIcon.text(icon.text[i]);
                        tempIcon.css("font-size", size+"px");
                    }
                } else if(type === "img") {
                    tempIcon = $("<img/>").attr("src", icon.url[i]).css("vertical-align", "bottom");
                }
                $icon.append(tempIcon);
            }
        }

        $icon.css("vertical-align", "text-bottom");

        return $icon;
    }

    this.hasToolbarIconType = function(type, optionalParameters) {
        var icon = this.getToolbarIconType(type, optionalParameters);
        return icon.class || icon.url;
    }

    this.getToolbarIconType = function(type, optionalParameters) {
        var icon = {
            type: "font",
            url: null,
            class: null,
            text: null
        }

        //TODO logic for user-specific toolbar icons?
        var materialPlusIcon = "add";
        if(type === "ENTRY") {
            icon.class = "material-icons";
            icon.text = materialPlusIcon + "subject";
        } else if(type === "FOLDER") {
            icon.class = "material-icons";
            icon.text = materialPlusIcon + "folder";
        } else if(type === "PROJECT") {
            icon.type = ["font", "img"];
            icon.url = [null, "./img/folder-with-settings.svg"];
            icon.class = ["material-icons", null];
            icon.text = [materialPlusIcon, null];
        } else if(type === "OTHER") {
            icon.class = "material-icons";
            icon.text = materialPlusIcon;
        } else if(type === "DATA") {
            icon.type = ["font", "font"]
            icon.class = ["material-icons", "fa fa-database"];
            icon.text = [materialPlusIcon, null];
        } else if(type === "EDIT") {
            icon.class = "glyphicon glyphicon-edit";
        } else if(type === "SAVE") {
            icon.class = "material-icons";
            icon.text = "save";
        } else if(type === "DELETE") {
            icon.class = "glyphicon glyphicon-trash";
        } else if(type === "CLEAR") {
            icon.class = "glyphicon glyphicon-remove";
        }  else if(type === "SEARCH") {
            icon.class = "glyphicon glyphicon-search";
            icon.class = "material-icons";
            icon.text = "search";
        } else if(type === "?") {
            icon.class = "material-icons";
            icon.text = "help";
        } else if(type === "PAGINATION_LEFT") {
            icon.class = "material-icons";
            icon.text = "navigate_before";
        } else if(type === "PAGINATION_RIGHT") {
            icon.class = "material-icons";
            icon.text = "navigate_next";
        } else if(type === "TEMPLATES") {
            icon.class = "material-icons";
            icon.text = "list_alt";
        } else if(type === "SPACE") {
            icon.type = ["font", "img"];
            icon.url = [null, "./img/folder-with-key.svg"];
            icon.class = ["material-icons", null];
            icon.text = [materialPlusIcon, null];
        } else if(type === "HIDE") {
            icon.class = "material-icons";
            icon.text = "keyboard_double_arrow_left";
        } else if(type === "SHOW") {
            icon.class = "material-icons";
            icon.text = "keyboard_double_arrow_right";
        } else if(type === "LOGIN") {
            icon.class = "glyphicon glyphicon-log-in";
        } else if(type === "LOGOUT") {
            icon.class = "glyphicon glyphicon-off";
        } else if(type === "BARCODE") {
            icon.class = "glyphicon glyphicon-barcode";
        } else if(type === "COLLAPSE_DOWN") {
            icon.class = "material-icons";
            icon.text = "keyboard_double_arrow_down";
        } else if(type === "COLLAPSE_UP") {
            icon.class = "material-icons";
            icon.text = "keyboard_double_arrow_up";
        } else if(type === "LAB_NOTEBOOK") {
            icon.class = "glyphicon glyphicon-book";
        } else if(type === "INVENTORY") {
            icon.class = "fa fa-cubes";
        } else if(type === "STOCK") {
            icon.class = "fa fa-shopping-cart";
        } else if(type === "UTILITIES") {
            icon.class = "glyphicon glyphicon-wrench";
        } else if(type === "PLUS") {
            icon.class = "material-icons";
            icon.text = "add";
        } else if(type === "MINUS") {
            icon.class = "material-icons";
            icon.text = "remove";
        } else if(type === "EYE_OPEN") {
            icon.class = "material-icons";
            icon.text = "visibility";
        } else if(type === "EYE_CLOSED") {
            icon.class = "material-icons";
            icon.text = "visibility_off";
        } else if(type === "FULLSCREEN") {
            icon.class = "glyphicon glyphicon-resize-full";
            icon.class = "material-icons";
            icon.text = "open_in_full";
        } else if(type === "FULLSCREEN_CLOSE") {
            icon.class = "material-icons";
            icon.text = "close_fullscreen";
        } else if(type === "FREEZE") {
            icon.class = "material-icons";
            icon.text = "ac_unit";
        }
        return icon;
    }

    this.getNavigationIcon = function(type, optionalParameters) {
        var icon = {
            type: "font",
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
        }  else if(type === "UTILITIES") {
            icon.class = "glyphicon glyphicon-wrench";
        } else if(type === "EXPORTS") {
            icon.class = "glyphicon glyphicon-export";
        } else if(type === "STORAGE_MANAGER") {
            icon.class = "glyphicon glyphicon-file";
        } else if(type === "USER_PROFILE") {
            icon.class = "glyphicon glyphicon-user";
        } else if(type === "GENERATE_BARCODES" || type === "BARCODE_GENERATOR" || type === "BARCODE") {
            icon.class = "glyphicon glyphicon-barcode";
        } else if(type === "SAMPLE_BROWSER" || type === "OBJECT_BROWSER") {
            icon.class = "glyphicon glyphicon-list-alt";
        } else if(type === "VOCABULARY_BROWSER") {
            icon.class = "glyphicon glyphicon-list-alt";
        } else if(type === "ADVANCED_SEARCH" || type === "SEARCH") {
            icon.class = "glyphicon glyphicon-search";
        } else if(type === "DROPBOX_MONITOR") {
            icon.class = "glyphicon glyphicon-info-sign";
        } else if(type === "ARCHIVING_HELPER") {
            icon.type = "img_with_class";
            icon.class = "fancytree-icon";
            icon.url = "./img/archive-not-requested-icon.png";
        } else if(type === "UNARCHIVING_HELPER") {
            icon.class = "glyphicon glyphicon-open";
        } else if(type === "CUSTOM_IMPORT" || type === "IMPORT") {
            icon.class = "glyphicon glyphicon-import";
        } else if(type === "USER_MANAGER") {
            icon.class = "fa fa-users";
        } else if(type === "USER_MANAGEMENT_CONFIG") {
            icon.class = "fa fa-users";
        } else if(type === "TRASHCAN") {
            icon.class = "glyphicon glyphicon-trash";
        } else if(type === "SETTINGS") {
            icon.class = "glyphicon glyphicon-cog";
        } else if(type === "OTHERTOOLS" || type === "OTHER_TOOLS") {
            icon.class = "glyphicon glyphicon-wrench";
        } else if(type === "EXPORT_TO_ZIP") {
            icon.class = "glyphicon glyphicon-export";
        } else if(type === "EXPORT_TO_RESEARCH_COLLECTION") {
            icon.type = "img_with_class";
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
                icon.type = "img";
                icon.url = "./img/folder-with-home.svg";
            }
            else {
                icon.type = "img";
                icon.url = "./img/folder-with-key.svg";

            }
        } else if(type === "SAMPLE") {
            if(optionalParameters && optionalParameters.sampleTypeCode) {
                var sampleTypeCode = optionalParameters.sampleTypeCode;
                if(sampleTypeCode === 'FOLDER') {
                    icon.class = "material-icons";
                    icon.text = "folder";
                } else {
                    var hasData = false;
                    if(optionalParameters.dataSets && optionalParameters.dataSets.length > 0) {
                        hasData = true;
                    }
                    if(optionalParameters.hasAfsFile) {
                        hasData = true;
                    }
                    if(optionalParameters.properties && Object.keys(optionalParameters.properties).length > 0) {
                        var hasMetadata = true;
                    }
                    if(hasData) {
                        if(hasMetadata) {
                            icon.type = "img";
                            icon.url = "./img/subject-with-attach_file.svg";

                        } else {
                            icon.class = "material-icons";
                            icon.text = "attach_file";
                        }
                    } else {
                        icon.class = "material-icons";
                        icon.text = "subject";
                    }

                }
            } else {
                icon.class = "material-icons";
                icon.text = "subject";
            }
        } else if(type === "EXPERIMENT") {
            if(optionalParameters) {
                if(optionalParameters.isExperimentWithoutChildren) {
                    icon.class = "fa fa-table";
                } else {
                    if(optionalParameters.dataSets && optionalParameters.dataSets.length > 0) {
                        var hasData = true;
                    }
                    if(optionalParameters.properties && Object.keys(optionalParameters.properties).length > 0) {
                        var hasMetadata = true;
                    }
                    if(hasData) {
                        if(hasMetadata) {
                            icon.type = "img";
                            icon.url = "./img/subject-with-attach_file.svg";
                        } else {
                            icon.class = "material-icons";
                            icon.text = "attach_file";
                        }
                    } else {
                        icon.class = "material-icons";
                        icon.text = "subject";
                    }
                }
            } else {
                icon.class = "material-icons";
                icon.text = "subject";
            }
        } else if(type === "PROJECT") {
            icon.type = "img";
            icon.url = "./img/folder-with-settings.svg";

        }

        return icon;
    };



}