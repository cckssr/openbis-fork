/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

define(["stjs"], function (stjs) {
  var ExportOptions = function(formats, xlsTextFormat, withReferredTypes, withImportCompatibility, zipSingleFiles,
                                withLevelsAbove, withLevelsBelow, withObjectsAndDataSetsParents, withObjectsAndDataSetsOtherSpaces) {
    this.formats = formats;
    this.xlsTextFormat = xlsTextFormat;
    this.withReferredTypes = withReferredTypes;
    this.withImportCompatibility = withImportCompatibility;
    this.zipSingleFiles = zipSingleFiles;
    this.withLevelsAbove = withLevelsAbove;
    this.withLevelsBelow = withLevelsBelow;
    this.withObjectsAndDataSetsParents = withObjectsAndDataSetsParents;
    this.withObjectsAndDataSetsOtherSpaces = withObjectsAndDataSetsOtherSpaces;
  }

  stjs.extend(
    ExportOptions,
    null,
    [],
    function (constructor, prototype) {
      prototype["@type"] = "as.dto.exporter.options.ExportOptions";

      constructor.serialVersionUID = 1;
      prototype.formats = null;
      prototype.xlsTextFormat = null;
      prototype.withReferredTypes = null;
      prototype.withImportCompatibility = null;
      prototype.zipSingleFiles = null;
      prototype.withLevelsAbove = null;
      prototype.withLevelsBelow = null;
      prototype.withObjectsAndDataSetsParents = null;
      prototype.withObjectsAndDataSetsOtherSpaces = null;

      prototype.getFormats = function() {
        return this.formats;
      };

      prototype.setFormats = function(formats) {
        this.formats = formats;
      };

      prototype.getXlsTextFormat = function() {
        return this.xlsTextFormat;
      };

      prototype.setXlsTextFormat = function(xlsTextFormat) {
        this.xlsTextFormat = xlsTextFormat;
      };

      prototype.isWithReferredTypes = function() {
        return this.withReferredTypes;
      };

      prototype.setWithReferredTypes = function(withReferredTypes) {
        this.withReferredTypes = withReferredTypes;
      };

      prototype.isWithImportCompatibility = function() {
        return this.withImportCompatibility;
      };

      prototype.setWithImportCompatibility = function(withImportCompatibility) {
        this.withImportCompatibility = withImportCompatibility;
      };

      prototype.isZipSingleFiles = function() {
        return this.zipSingleFiles;
      };

      prototype.setZipSingleFiles = function(zipSingleFiles) {
        this.zipSingleFiles = zipSingleFiles;
      };

      prototype.isWithLevelsAbove = function() {
        return withLevelsAbove;
      };

      prototype.setWithLevelsAbove = function(withLevelsAbove) {
        this.withLevelsAbove = withLevelsAbove;
      };

      prototype.isWithLevelsBelow = function() {
        return withLevelsBelow;
      };

      prototype.setWithLevelsBelow = function(withLevelsBelow) {
        this.withLevelsBelow = withLevelsBelow;
      };

      prototype.isWithObjectsAndDataSetsParents = function() {
        return withObjectsAndDataSetsParents;
      };

      prototype.setWithObjectsAndDataSetsParents = function(withObjectsAndDataSetsParents) {
        this.withObjectsAndDataSetsParents = withObjectsAndDataSetsParents;
      };

      prototype.isWithObjectsAndDataSetsOtherSpaces = function() {
        return withObjectsAndDataSetsOtherSpaces;
      };

      prototype.setWithObjectsAndDataSetsOtherSpaces = function(withObjectsAndDataSetsOtherSpaces) {
        this.withObjectsAndDataSetsOtherSpaces = withObjectsAndDataSetsOtherSpaces;
      };

    },
    {
      formats: {
        name: "Set",
        arguments: ["ExportFormat"]
      },
      xlsTextFormat: "XlsTextFormat",
      withReferredTypes: "Boolean",
      withImportCompatibility: "Boolean"
    }
  );

  return ExportOptions;
});