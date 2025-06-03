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
function HierarchyFilterView(controller, model, viewId) {
	this._controller = controller;
	this._model = model;
	this._viewId = viewId;
	
	this.init = function(container) {
		var $filtersForm = $('<form>' , { id : 'hierarchy-filter-' + this._viewId, class : 'form-inline'});
		container.append($filtersForm);
        var action = function(event) {
            this._model.action(); event.preventDefault();
        }
        $("body").on("submit", "#hierarchy-filter-" + this._viewId, action)

		var maxChildren = this._model.getMaxChildrenDepth();
		var $filtersFormSliderChildren = null;
		if(maxChildren > 0) {
			$filtersFormSliderChildren = $('<input>' , { 'id' : 'childrenLimit-' + this._viewId , 'type' : 'text' , 'class' : 'span2', 'value' : '' , 'data-slider-max' : maxChildren , 'data-slider-value' : maxChildren, 'width' : '100px'});
		} else {
			$filtersFormSliderChildren = 'No Children';
		}
		
		var maxParents = this._model.getMaxParentsDepth();
		var $filtersFormSliderParents = null;
		if(maxParents > 0) {
			$filtersFormSliderParents = $('<input>' , { 'id' : 'parentsLimit-' + this._viewId , 'type' : 'text' , 'class' : 'span2', 'value' : '' , 'data-slider-max' : maxParents , 'data-slider-value' : maxParents, 'width' : '100px'});
		} else {
			$filtersFormSliderParents = 'No Parents';
		}
		
		var types = this._model.getTypes();
		var $filtersFormEntityTypes = $('<select>', { 'id' : 'entityTypesSelector-' + this._viewId , class : 'multiselect' , 'multiple' : 'multiple'});
		for (var type in types) {
			$filtersFormEntityTypes.append($('<option>', { 'value' : type , 'selected' : ''}).html(Util.getDisplayNameFromCode(type)));
		}
		
		$filtersForm
			.append('<b>Filters</b>')
			.append("<span style='padding-right:15px;'></span>")
			.append('Children: ')
			.append($filtersFormSliderChildren)
			.append("<span style='padding-right:15px;'></span>")
			.append(' Parents: ')
			.append($filtersFormSliderParents)
			.append("<span style='padding-right:15px;'></span>")
			.append(' Show Types: ')
			.append($filtersFormEntityTypes);
	}
}