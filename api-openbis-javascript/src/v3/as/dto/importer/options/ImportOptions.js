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
  var ImportOptions = function() {
  }

  stjs.extend(
    ImportOptions,
    null,
    [],
    function (constructor, prototype) {
      prototype["@type"] = "as.dto.importer.options.ImportOptions";

      constructor.serialVersionUID = 1;
      prototype.mode = null;
      prototype.experimentsByType = null;
      prototype.spacesByType = null;

      prototype.getMode = function() {
        return this.mode;
      };

      prototype.setMode = function(mode) {
        this.mode = mode;
      };

      prototype.getExperimentsByType = function() {
          return this.experimentsByType;
      };

        prototype.setExperimentsByType = function(experimentsByType) {
            this.experimentsByType = experimentsByType;
        };

        prototype.getSpacesByType = function() {
            return this.spacesByType;
        };

        prototype.setSpacesByType = function(spacesByType) {
            this.spacesByType = spacesByType;
        };
    },
    {
      mode: "ImportMode",
      experimentsByType: {
        name : "Map",
        arguments : [ "String", "String" ]
      },
      spacesByType: {
        name : "Map",
        arguments : [ "String", "String" ]
      }
    }
  );

  return ImportOptions;
});