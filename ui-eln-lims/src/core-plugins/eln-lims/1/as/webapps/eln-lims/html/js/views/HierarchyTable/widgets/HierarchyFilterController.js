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
function HierarchyFilterController(entity, action) {
    this._viewId = mainController.getNextId();
	this._model = new HierarchyFilterModel(entity, action);
	this._view = new HierarchyFilterView(this, this._model, this._viewId);
	this._container;

	this.init = function(container) {
		this._view.init(container);
		this._container = container;
		var act = function(event){
            action();
        }

		this._container.find('#childrenLimit-' + this._viewId).slider().on('slideStop', act);
		this._container.find('#parentsLimit-' + this._viewId).slider().on('slideStop', act);
		this._container.find('#entityTypesSelector-' + this._viewId).multiselect();
		this._container.find('#entityTypesSelector-' + this._viewId).change(act);

	}

	this.getParentsLimit = function() {
		return getSliderValue("parentsLimit-" + this._viewId);
	}
	
	this.getChildrenLimit = function() {
		return getSliderValue("childrenLimit-" + this._viewId);
	}
	
	this.getSelectedEntityTypes = function() {
		var selectedEntityTypes = $('#entityTypesSelector-' + this._viewId).val();
		if(!selectedEntityTypes) {
			selectedEntityTypes = [];
		}
		return selectedEntityTypes;
	}
	
	var getSliderValue = function(id) {
		var element = $('#' + id)
		if(element.length > 0) {
		    if(element.data('slider')) {
		        return element.data('slider').getValue();
		    }
		}
		return  0;
	}

}