/*
 * Copyright 2015 ETH Zuerich, Scientific IT Services
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
function HierarchyFilterModel(entity) {
	this._entity = entity;
	
	this.getTypes = function() {
		var types = {};
		var visited = {};
		var getTypesWithRecursion = function(entity, types, visited) {
			var permId = FormUtil.getPermId(entity);
			var type = FormUtil.getType(entity);
			
			if(!visited[permId]) {
				visited[permId] = true;
			} else {
				return;
			}
			if (!types[type]) {
				types[type] = true;
			}
			if(entity.parents) {
				for (var i = 0; i < entity.parents.length; i++) {
					getTypesWithRecursion(entity.parents[i], types, visited);
				}
			}
			if (entity.children) {
				for (var i = 0; i < entity.children.length; i++) {
					getTypesWithRecursion(entity.children[i], types, visited);
				}
			}
		}
		getTypesWithRecursion(this._entity, types, visited);
		return types;
	}
	
	this.getMaxChildrenDepth = function() {
	    var visited = {} // This is to avoid loops
		var getMaxChildrenDepthRecursion = function(entity, max) {
			if (entity.children && !visited[entity.permId]) {
			    visited[entity.permId] = true;
				var possibleNextMax = [];
				for (var i = 0; i < entity.children.length; i++) {
					var nextMax = getMaxChildrenDepthRecursion(entity.children[i], (max + 1));
					possibleNextMax.push(nextMax);
				}
				for (var i = 0; i < possibleNextMax.length; i++) {
					if (possibleNextMax[i] > max) {
						max = possibleNextMax[i];
					}
				}
			}
			return max;
		}
		return getMaxChildrenDepthRecursion(this._entity, 0);
	}
	
	this.getMaxParentsDepth = function(sample) {
	    var visited = {} // This is to avoid loops
		var getMaxParentsDepthRecursion = function(entity, max) {
			if (entity.parents && !visited[entity.permId]) {
			    visited[entity.permId] = true;
				var possibleNextMax = [];
				for (var i = 0; i < entity.parents.length; i++) {
				    var permId = entity.parents[i].permId;
                    var nextMax = getMaxParentsDepthRecursion(entity.parents[i], (max + 1));
                    possibleNextMax.push(nextMax);
				}
				for (var i = 0; i < possibleNextMax.length; i++) {
					if (possibleNextMax[i] > max) {
						max = possibleNextMax[i];
					}
				}
			}
			return max;
		}
		return getMaxParentsDepthRecursion(this._entity, 0);
	}
}
