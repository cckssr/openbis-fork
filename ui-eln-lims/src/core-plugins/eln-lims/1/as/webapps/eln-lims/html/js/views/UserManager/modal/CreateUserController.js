/*
 * Copyright 2014-2025 ETH Zuerich, Scientific IT Services
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

function CreateUserController(authenticationService) {
	this._createUserModel = new CreateUserModel(authenticationService);
	this._createUserView = new CreateUserView(this, this._createUserModel);
	var codeRegExp = new RegExp("^[A-Za-z0-9_\\-\\.]+$");
	
	this.init = function() {
		this._createUserView.repaint();
	}
	
	this.createUser = function() {
		var _this = this;
		this._createUserView.disableAccept();

			var createUser = function() {
                if(!codeRegExp.test(_this._createUserModel.userId)) {
                    Util.showUserError("Please use allowed characters for ELN user creation (allowed: A-Z, a-z, 0-9 and _, -, .)", function() {
                        _this._createUserView.enableAccept();
                    }, true);
                    return;
                }

				mainController.serverFacade.createELNUser(_this._createUserModel.userId, function(isRegistered, message) {
					if(isRegistered) {
						Util.showSuccess(message, function() {
							Util.unblockUI();
							mainController.changeView("showUserManagerPage");
						});
					} else if (message.indexOf("constraint [pers_bk_uk]") !== -1){
						Util.showUserError("Person already exist", function() {
							_this._createUserView.enableAccept();
						}, true);
					} else {
						Util.showUserError(message, function() {
                            _this._createUserView.enableAccept();
                        }, true);
					}
				});
			}
			
			if(!this._createUserModel.isPasswordRequired) {
				createUser();
			} else {
				if(_this._createUserModel.password === _this._createUserModel.passwordRepeat) {
					mainController.serverFacade.registerUserPassword(
							_this._createUserModel.userId,
							_this._createUserModel.password,
							function(isRegistered) {
								if(isRegistered) {
									createUser();
								} else {
									Util.showError("User can't be created, check with your administator.", function() {
										_this._createUserView.enableAccept();
									}, true);
								}
							});
				} else {
					Util.showUserError("Passwords are not equal.", function() { _this._createUserView.enableAccept(); }, true);
				}
			}
		
	}
}