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

var LabelUtil = new function() {

    const _toolbarLabels = {
        ARCHIVE: { label: "Archive", tooltip: "" },
        BARCODE: { label: "Barcode/QR Code Print", tooltip: "" },
        BARCODE_UPDATE: { label: "Custom Barcode/QR Code Update", tooltip: "" },
        COPY: { label: "Copy", tooltip: "Copy" },
        CREATE_ENTRY: { label: "Entry", tooltip: "New Entry" },
        CREATE_FOLDER: { label: "Folder", tooltip: "New Folder" },
        CREATE_OTHER: { label: "Other", tooltip: "Create different object" },
        CREATE_PROJECT: { label: "Project", tooltip: "Create project" },
        DELETE: { label: "Delete", tooltip: "Delete" },
        DOCS: { label: null, tooltip: "Documentation" },
        EDIT: { label: "Edit", tooltip: "Edit entity" },
        EDIT_DATA: { label: "Edit", tooltip: "Edit data" },
        EDIT_COLLECTION: { label: "Edit", tooltip: "Edit collection" },
        EDIT_OBJECT: { label: "Edit", tooltip: "Edit object" },
        EDIT_PROJECT: { label: "Edit", tooltip: "Edit project" },
        EDIT_SPACE: { label: "Edit", tooltip: "Edit space" },
        EXPORT: { label: "Export", tooltip: "" },
        EXPORT_ALL: { label: "Export", tooltip: "" },
        FREEZE: { label: "Freeze", tooltip: "Freeze Entity data (Disable further data upload)" },
        FREEZE_ENTITY: { label: "Freeze Entity (Disable further modifications)", tooltip: "" },
        FROZEN: { label: "Frozen", tooltip: "Entity Frozen" },
        FROZEN_DATA: { label: "Frozen Data", tooltip: "Freeze Entity metadata (Disable further modifications)" },
        HIERARCHY_GRAPH: { label: "Hierarchy Graph", tooltip: "" },
        HIERARCHY_TABLE: { label: "Hierarchy Table", tooltip: "" },
        HISTORY: { label: "History", tooltip: "" },
        METADATA_IMPORT_TEMPLATE: { label: "Template for metadata import", tooltip: "" },
        MANAGE_ACCESS: { label: "Manage access", tooltip: "" },
        MOVE: { label: "Move", tooltip: "Move" },
        NEW_JUPYTER: { label: "New Jupyter notebook", tooltip: "" },
        PRINT: { label: "Print PDF", tooltip: "" },
        SAVE: { label: "Save", tooltip: "Save changes" },
        TEMPLATES: { label: "Templates", tooltip: "" },
        UPLOAD_DATASET: { label: "Dataset", tooltip: "Upload dataset" },
        UPLOAD_DATASET_HELPER: { label: "Dataset upload helper tool for eln-lims dropbox", tooltip: "" },
    }

    this.getToolbarLabelInfo = function(toolbarButtonName) {
        if(toolbarButtonName in _toolbarLabels) {
            return _toolbarLabels[toolbarButtonName];
        }
        return { label: toolbarButtonName, tooltip: ""};
    }

}