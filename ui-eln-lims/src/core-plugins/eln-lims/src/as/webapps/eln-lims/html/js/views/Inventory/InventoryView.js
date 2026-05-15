/*
 * Copyright 2016 ETH Zuerich, Scientific IT Services
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

function InventoryView(inventoryController, inventoryView) {
	this.inventoryController = inventoryController;
	this.inventoryView = inventoryView;
	this._viewId = mainController.getNextId();

	this.repaint = function(views) {
        var _this = this;
		var $form = $("<div>");
		var $formColumn = $("<div>");
			
		$form.append($formColumn);
		
		var $formTitle = $("<h2>").append(profile.MainMenuNodeNames.Inventory);
		
		//
		// Toolbar
		//
		var toolbarModel = [];
		
		mainController.serverFacade.listSpaces(function(spaces) {
	            var labSpaces = [];
				for (var i = 0; i < spaces.length; i++) {
	                var space = spaces[i];
                    if(profile.isInventorySpace(space) && !space.endsWith("STOCK_CATALOG") 
                            && !space.endsWith("STOCK_ORDERS") && !space.endsWith("ELN_SETTINGS")) {
                        labSpaces.push({ type: "SPACE", permId : space, expand : true });
                    }
	            }
	            
                if (profile.isAdmin) {
                    var $createSpace = FormUtil.getToolbarButton("SPACE", function() {
                                 _this.inventoryController.createSpace();
                             }, "Space", "New Inventory Space", "create-btn-" + this._viewId, 'btn btn-primary btn-secondary');
                    toolbarModel.push({component : $createSpace});
                }
			
				views.header.append(FormUtil.getToolbar(toolbarModel));
		});
		
		views.header.append($formTitle);
		views.content.append($formColumn);
	}
}