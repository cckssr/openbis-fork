/*
 * Copyright 2014 - 2024 ETH Zuerich, Scientific IT Services
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
 * Class MainController
 * 
 * This class is used as central point of control into the application.
 *
 * It holds:
 * - server facade
 * - Configuration profile
 * - Atributes used by inline HTML/Javascript
 * - enterApp method
 * - showView methods
 *
 * @constructor
 * @this {MainController}
 * @param {DefaultProfile} profile Profile used to configure the app.
 */
function MainController(profile) {
	//
	// Atributes
	//
	
	this.openbisV1 = new openbis();
	this.openbisV3 = null;


	this.loadV3 = function(callback) {
	    this.serverFacade.getOpenbisV3(function(openbisV3) {
            openbisV3._private.sessionToken = mainController.serverFacade.getSession();
            if(openbisV3._private.log) {
            	openbisV3._private.log = function() {
                	// Disables v3 logging
                };
            }
            mainController.openbisV3 = openbisV3;
            callback();
	    });
	};
	
	// Server Facade Object
	this.serverFacade = new ServerFacade(this.openbisV1); //Client APP Facade, used as control point to know what is used and create utility methods.

	// Configuration
	this.profile = profile;
	this.profile.serverFacade = this.serverFacade;
	FormUtil.profile = this.profile;
	
	// Attributes - Widgets typically hold both the model and the view, they are here so they can be accessed by inline HTML/Javascript when needed.
	
	// Controllers currently being used
	this.sideMenu = null;
	this.currentView = null;
	this.globalEventListeners = function() {
	    const map = {};
	    const funcMap = {};

	    return {
            addEventListener: (event, func) => {
                  if(func) {
                    var anonFunc = (parameters) => map[event](parameters);
                    !map[event] && document.addEventListener(event, anonFunc);
                    map[event] = func;
                    funcMap[event] = anonFunc;
                  }
                },
            getEventListener: (event) => {
                return map[event] ?? null;
            },
            removeEventListener: (event) => {
                document.removeEventListener(event, funcMap[event]);
                delete map[event];
                delete funcMap[event];
            }
	    };
	}();

	this._counter = 1;
	this.getNextId = function() {
	    return this._counter++;
	}
	
	// Views currently being displayed
	this.views = {
			menu : null,
			mainHeader: null,
			header : null,
			content : null,
			tabContent : null,
			auxContent : null
	}
	
	//Refresh Functionality
	this.lastViewChange = null;
	this.lastArg = null;
	this.refreshView = function() {
		this.changeView(this.lastViewChange, this.lastArg);
	}
	
	//Functionality to keep state
	this.backStack = [];
    this._stateCounter = 0;
    this._backStackLimit = 30;
	this.loggedInAnonymously = false;

	this.zenodoApiTokenKey = "personal-zenodo-api-token";
	
	//
	// Validates and enters the app
	//

	this.enterApp = function(data, username, password) {
        sessionStorage.removeItem("normalLoginHasBeenForces");
	    var _this = this;
	    if(data && !username && !password) {
	        this.openbisV1.listDataStores(function(result) {
	            if(result && result.error && result.error.message) {
	                var callback = function() {Util.unblockUI();};
	                Util.showUserError(result.error.message, callback);
	            } else {
	                _this.initApp(data, username, password);
	            }
	        });
	    } else {
	        this.initApp(data, username, password);
	    }
	}

	this.initAppBeforeLogin = function(callback){
		this.serverFacade.getServerPublicInformation(function(serverInformation){
			this.profile.singleSignOnUrlTemplate = serverInformation['authentication-service.switch-aai.link'] || this.profile.singleSignOnUrlTemplate
			this.profile.singleSignOnLinkLabel = serverInformation['authentication-service.switch-aai.label'] || this.profile.singleSignOnLinkLabel || 'SWITCHaai Single Sign On Login Service'
			callback()
		})
	}

	this.initApp = function(data, username, password) {
		var localReference = this;
		//
		// Check
		//

		//
		// Check Credentials
		//
		if(data.result == null){
			$("#username").focus();
			var callback = function() {Util.unblockUI();};
			var msg = 'The given username or password is not correct.';
			if (data.error && data.error.message) {
				msg = data.error.message;
			}
			Util.showUserError(msg, callback);
			this.serverFacade.doIfFileAuthenticationService((function() {
				this._enablePasswordResetLink();
			}).bind(this));
			return;
		}

		//
		// Back Button Logic
		//
		
		//BackButton Logic
		var _this = this;
		this.backButtonLogic = function(e) {
//			// The back button is used to restore standard size before doing any other action
//			if(LayoutManager.fullScreenFlag) {
//                LayoutManager.restoreStandardSize();
//                return;
//			}
			// Standard back button logic
			if(	this.currentView && 
				this.currentView.finalize) {
				this.currentView.finalize(true);
			}
			var back = true;
			if(e.state && e.state.id) {
			    if(e.state.id > this._stateCounter) {
			        // forward logic
			        back = false;
			        var toPush = null;
                    toPush = {
                            header : null,
                            content : null,
                            auxContent : null
                    }

                    if(this.views.header) {
                        toPush.header = this.views.header;
                        toPush.header.detach();
                    }

                    if(this.views.content) {
                        toPush.content = this.views.content;
                        toPush.content.detach();
                    }

                    if(this.views.auxContent) {
                        toPush.auxContent = this.views.auxContent;
                        toPush.auxContent.detach();
                    }

                    this.backStack.push({
                        view : toPush,
                        url : window.location.href,
                        controller: this.currentView,
                    });
                    if(this.backStack.length > this._backStackLimit) {
                        this.backStack.shift();
                    }
			    }
                this._stateCounter = e.state.id;
			}
			
			var toPop = null;
			if(back && this.backStack.length > 0) {
				toPop = this.backStack.pop();
			}

			if(toPop && toPop.view !== null) {
				this.views.header = toPop.view.header;
				this.views.content = toPop.view.content;
				this.views.auxContent = toPop.view.auxContent;
				if(toPop.controller !== null) {
                    this.currentView = toPop.controller;
                    if(this.currentView.restoreEvents)
                    {
                        this.currentView.restoreEvents();
                    }
                }
				LayoutManager.reloadView(this.views);
			} else {
				function isEmpty(value) {
                    return value === null || value === undefined || value === "null" || value === "undefined"
                }

                var queryString = Util.queryString();
                var menuUniqueId = queryString.menuUniqueId;
                var viewName = queryString.viewName;
                var viewData = queryString.viewData

                if(isEmpty(viewName)){
                    viewName = null
                }

                if(isEmpty(viewData)){
                    viewData = null
                }

                if(viewName) {
                    localReference._changeView(viewName, viewData, false, false);
                    localReference.sideMenu.moveToNodeId(decodeURIComponent(menuUniqueId));
                }
			}
		}
		
		window.addEventListener("popstate", function(e) {
			_this.backButtonLogic(e);
		}); 
		
		//
		// Start App if credentials are ok
		//
		$('body').removeClass('bodyLogin');
		$("#login-form").hide();
		$("#login-form-sub").hide();
		$("#main").show();
		
        //Get Metadata from all sample types before showing the main menu
        this.loadV3(function() {
            _this.serverFacade.listSampleTypes (
                    function(result) {
                        //Load Sample Types
                        localReference.profile.allSampleTypes = result.result;
                        
                        //Load datastores for automatic DSS configuration, the first one will be used
                        localReference.serverFacade.listDataStores(function(dataStores) {
                                localReference.profile.allDataStores = dataStores.result;
                                
                                var nextInit = function() {
                                    //Load display settings
                                    localReference.serverFacade.getUserDisplaySettings( function(response) {
                                        if(response.result) {
                                            localReference.profile.displaySettings = response.result;
                                        }
                                        
                                        //Load Experiment Types
                                        localReference.serverFacade.listExperimentTypes(function(experiments) {
                                            localReference.profile.allExperimentTypes = experiments.result;
                                            
                                            //Load DataSet Types
                                            localReference.serverFacade.listDataSetTypes(function(dataSetTypes) {
                                                localReference.profile.allDataSetTypes = dataSetTypes.result;

                                                //Init profile
                                                var startAppFunc = function() {
                                                    //Start App
                                                    localReference.sideMenu = new SideMenuWidgetController(localReference);
                                                    localReference.mainHeader = new MainHeaderController(localReference);
                                                    localReference.views.mainHeader = $("<div>");

                                                    localReference.mainHeader.init(localReference.views.mainHeader, () => {});
                                                    localReference.views.menu = $("<div>");
                                                    localReference.views.tabContent = $("<div>");
                                                    localReference.tabContent = new TabContentController(localReference);

                                                    localReference.tabContent.init(localReference.views.tabContent, () => {});
//
                                                    localReference.sideMenu.init(localReference.views.menu, function() {
                                                        //Page reload using the URL info
                                                        var queryString = Util.queryString();
                                                        var menuUniqueId = queryString.menuUniqueId;
                                                        var viewName = queryString.viewName;
                                                        var viewData = queryString.viewData;
                                                        var hideMenu = queryString.hideMenu;
                                                        
                                                        LayoutManager.reloadView(localReference.views);
//                                                        localReference.views.tabContent = LayoutManager.tabContent;
                                                        if(viewName && viewData) {
                                                            localReference.sideMenu.moveToNodeId(decodeURIComponent(menuUniqueId)).then(function(){
                                                                localReference.changeView(viewName, viewData);
                                                            })
                                                        } else {
                                                             profile.getHomeSpace(function(homeSpaceCode) {
                                                                if(homeSpaceCode) {
                                                                    localReference.serverFacade.getSetting('hasLoggedOnce', function(hasLoggedOnce) {
                                                                        if(hasLoggedOnce !== 'true') {
                                                                            localReference.serverFacade.setSetting('hasLoggedOnce', true);
                                                                            var homeSpace = {
                                                                                type: "SPACE",
                                                                                id: homeSpaceCode
                                                                            }
                                                                            localReference.sideMenu._browserController.load().then(x => {
                                                                                    localReference.sideMenu.moveToNodeId(JSON.stringify(homeSpace)).then(function(){
                                                                                        localReference.sideMenu.setAsRootById("LAB_NOTEBOOK");
                                                                                    })
                                                                            });
                                                                        }
                                                                    });
                                                                } else {
                                                                    localReference.sideMenu._browserController.load().then(x => {
                                                                        labNotebookNode = {
                                                                            type: "LAB_NOTEBOOK",
                                                                            id: "LAB_NOTEBOOK"
                                                                        }
                                                                        localReference.sideMenu.moveToNodeId(JSON.stringify(labNotebookNode));
                                                                    });
                                                                }
                                                            });
                                                            localReference.changeView("showLabNotebookPage", null);
                                                        }
                                                        
                                                        Util.unblockUI();
                                                        LayoutManager.resize(mainController.views, true); // Maybe fixes white screen on startup?

                                                        // Keep Alive
                                                        localReference.serverFacade.scheduleKeepAlive();

                                                    });
                                                };

                                                localReference.profile.init(startAppFunc);
                                            });
                                        });
                                    });
                                }
                                
                                nextInit();
                        });
                    }
                );
        });
	}

	this._enablePasswordResetLink = function() {

        var userId = $("#username").val();
	    var $container = $("#password-reset-container");
	    $container.empty();

	    $resetLink = $("<a>").text("reset password by email for user " + userId);
	    $container.append($resetLink);
	    var height = $container.height();
	    $container.css({ "margin-top" : -height + "px" });

	    $resetLink.on("click", (function() {
	        Util.blockUI();
    	    this.serverFacade.sendResetPasswordEmail(userId, function() {
                Util.unblockUI();
                Util.showInfo("An email with instructions how to reset the password has been sent to " + userId + " if this user exists.");
    	    });

	    }).bind(this));

	}

	this.resetPasswordRequested = function() {
        var queryString = Util.queryString();
        return queryString.resetPassword == "true";
	}

	this.resetPassword = function() {
        var queryString = Util.queryString();
        var userId = queryString.userId;
        var token = queryString.token;

        if (userId && token) {
            Util.blockUI();
            this.serverFacade.resetPassword(userId, token, function() {
                Util.unblockUI();
                Util.showInfo("A new password has been sent as an email to user " + userId + " if this user exists.");                
            });
        } else {
            Util.showUserError("To reset the password, the parameters 'userId' and 'token' need to be set.");
        }
	}

	//
	// authorization
	//

	// params.space: space code
	// params.project: (optional) project code
	// params.callback: function to be called with the role list
	this.getUserRole = function(params, callback) {
		if (profile.isAdmin) {
			callback(["ADMIN"]);
		} else {
			var roles = [];
			mainController.serverFacade.searchRoleAssignments({
				user: mainController.serverFacade.getUserId(),
			}, function(roleAssignments) {
				for (var i=0; i<roleAssignments.length; i++) {
					var ra = roleAssignments[i];
					if (ra.space && ra.space.code == params.space
							&& roles.indexOf(ra.role) < 0) {
						roles.push(ra.role);
					}
					if (ra.project && params.project && ra.project.code == params.project
							&& roles.indexOf(ra.role) < 0) {
						roles.push(ra.role);
					}
				}
				callback(roles);
			}, function(errorResult) {
				callback([]);
			});
		}
	}

	// gets all role assignments for one space or project (all users)
	// params.space: space for which the role assignments should be loaded
	// params.project: project for which the role assignments should be loaded
	this.getRoleAssignments = function(params, callback) {
		mainController.serverFacade.searchRoleAssignments({
			space: params.space,
			project: params.project,
		}, function(roleAssignments) {
			callback(roleAssignments);
		});
	}

	// role, grantTo, groupOrUser, spaceCode, projectPermId
	this.authorizeUserOrGroup = function(params, callback) {
		mainController.serverFacade.createRoleAssignment(params, callback);
	}

	this.deleteRoleAssignment = function(roleAssignmentTechId, callback) {
		mainController.serverFacade.deleteRoleAssignment(roleAssignmentTechId, callback);
	}

	//
	// Main View Changer - Everything on the application rely on this method to alter the views, arg should be a string
	//
	this.changeView = function(newViewChange, arg, shouldStateBePushToHistory) {
		this._changeView(newViewChange, arg, true, shouldStateBePushToHistory);
	}

	this._changeView = function(newViewChange, arg, shouldURLBePushToHistory, shouldStateBePushToHistory) {
		LayoutManager.isBlocked = false;
		var _this = this;
		var _next = function() {
		    Util.unblockUI();
		    _this._changeViewNext(newViewChange, arg, shouldURLBePushToHistory, shouldStateBePushToHistory);
		}
		//
		// Dirty forms management, to avoid loosing changes.
		//
		var discardChanges = null;
		if( this.currentView &&
			this.currentView.isDirty &&
			this.currentView.isDirty()) {
			//Ask the user if wants to leave the view in case is dirty
			Util.blockUIConfirm("Leaving this window will discard any changes, are you sure?", _next, null /* Do nothing */);
		} else {
		    _next();
		}
	}

	this._changeViewNext = function(newViewChange, arg, shouldURLBePushToHistory, shouldStateBePushToHistory) {
		LayoutManager.isBlocked = false;

		//
		// Finalize view, used to undo mayor modifications to how layout and events are handled
		//
		
		if(	this.currentView && 
			this.currentView.finalize) {
			this.currentView.finalize(false);
		}
		
		CKEditorManager.destroy();
		this.sideMenu.removeSubSideMenu();

		//
		// Obtain real argument
		//
        try {
            var cleanText = decodeURIComponent(arg); //If the JSON is written on the URL we need to clean special chars
            var argToUse = JSON.parse(cleanText);
		    arg = argToUse;
	    } catch(err) {
		    // unchanged arg
        }

		//
		// Permanent URLs
		//
		if (shouldURLBePushToHistory) {
			var menuUniqueId = this.sideMenu.getCurrentNodeId();
			var url = Util.getURLFor(menuUniqueId, newViewChange, arg);
			history.pushState({id: ++this._stateCounter}, "", url); //History Push State

			var toPush = null;
			if(shouldStateBePushToHistory) {
				toPush = {
						header : null,
						content : null,
						auxContent : null
				}

				if(this.views.header) {
					toPush.header = this.views.header;
					//toPush.header.detach();
				}

				if(this.views.content) {
					toPush.content = this.views.content;
//					toPush.content.detach();
				}

				if(this.views.auxContent) {
					toPush.auxContent = this.views.auxContent;
//					toPush.auxContent.detach();
				}
			}

			this.backStack.push({
				view : toPush,
				url : url,
				controller: this.currentView,
			});
			if(this.backStack.length > this._backStackLimit) {
                this.backStack.shift();
            }
		}

		//
		//
		//
		var refreshPageMessage = "The item is no longer available, refresh the page, if the problem persists tell your admin."
		try {
			switch (newViewChange) {
			    case "showBarcodesGeneratorPage":
			        document.title = "Barcodes Generator";
			        this.mainHeader.navigateToTab("TOOLS");
			        var initialized = false;
			        var barcodesGeneratorViews = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('BARCODE_GENERATOR'), () => {initialized=true});

			        var controller = BarcodeUtil.preGenerateBarcodes(barcodesGeneratorViews);

			        this.currentView = controller;
			        break;
				case "showJupyterWorkspace":
					document.title = "Jupyter Workspace";
					this.mainHeader.navigateToTab("TOOLS");
					var views = this._getNewViewModel(false, true, false, TabContentUtil.getToolTabInfo('JUPYTER_WORKSPACE'));
					var userId = this.serverFacade.getUserId();
					var url = JupyterUtil.getJupyterURL();
					views.content.append("Opening new tab/window with your Jupyter Workspace, please allow pop ups: " + url);
					var win = window.open(url, '_blank');
					win.focus(); 
					this.currentView = null;
					break;
				case "showNewJupyterNotebookCreator":
				    this.mainHeader.navigateToTab("TOOLS");
					var jupyterNotebook = new JupyterNotebookController();
					jupyterNotebook.init();
					break;
				case "showUserProfilePage":
					document.title = "User Profile";
					this.mainHeader.navigateToTab("TOOLS");
					this._showUserProfilePage(FormMode.VIEW);
					break;
				case "showEditUserProfilePage":
					document.title = "Edit User Profile";
					this.mainHeader.navigateToTab("TOOLS");
					this._showUserProfilePage(FormMode.EDIT);
					break;
				case "showOtherToolsPage":
					document.title = "Other Tools";
					this.mainHeader.navigateToTab("TOOLS");
					this._showOtherToolsPage();
					break;
				case "showSettingsPage":
				    document.title = "Settings";
				    this.mainHeader.navigateToTab("TOOLS");
				    this._showSettingsPage(FormMode.VIEW, arg);
				    break;
				case "showEditSettingsPage":
					document.title = "Edit Settings";
					this.mainHeader.navigateToTab("TOOLS");
					this._showSettingsPage(FormMode.EDIT, arg);
					break;
                case "showCustomImportPage":
                    document.title = "Custom Import";
                    this.mainHeader.navigateToTab("TOOLS");
                    this._showCustomImportPage();
                    break;
				case "showExportTreePage":
					document.title = "Export Builder";
					this.mainHeader.navigateToTab("TOOLS");
					var newExportView = new ExportTreeController(this);
					var exportViews = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('EXPORT'));
					newExportView.init(exportViews);
					this.currentView = newExportView;
					break;
				case "showResearchCollectionExportPage":
					document.title = "Research Collection Export Builder";
					this.mainHeader.navigateToTab("TOOLS");
					var newResearchCollectionExportView = new ResearchCollectionExportController(this);
					var researchCollectionExportViews = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('RESEARCH_COLLECTION'));
					newResearchCollectionExportView.init(researchCollectionExportViews);
					this.currentView = newResearchCollectionExportView;
					break;
				case "showZenodoExportPage":
					document.title = "Zenodo Export Builder";
					this.mainHeader.navigateToTab("TOOLS");
					var newZenodoExportView = new ZenodoExportController(this);
					var zenodoExportViews = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('ZENODO_EXPORT'));
					newZenodoExportView.init(zenodoExportViews);
					this.currentView = newZenodoExportView;
					break;
				case "showLabNotebookPage":
					document.title = "Lab Notebook";
					this.mainHeader.navigateToTab("LAB_NOTEBOOK");
					var newView = new LabNotebookController(this);
					var views = this._getNewViewModel(true, true, false, TabContentUtil.getTopLevelTabInfo("LAB_NOTEBOOK"));
					newView.init(views);
					this.currentView = newView;
					break;
				case "showInventoryPage":
					document.title = "Inventory";
					this.mainHeader.navigateToTab("LIMS");
					var newView = new InventoryController(this);
					var views = this._getNewViewModel(true, true, false, TabContentUtil.getTopLevelTabInfo("INVENTORY"));
					newView.init(views);
					this.currentView = newView;
					break;
				case "showAdvancedSearchPage":
					document.title = "Advanced Search";
					this.mainHeader.navigateToTab("TOOLS");
					this._showAdvancedSearchPage(arg);
					break;
                case "showDropboxMonitorPage":
                    document.title = "Dropbox Monitor";
                    this.mainHeader.navigateToTab("TOOLS");
                    this._showDropboxMonitor();
                    break;
				case "showArchivingHelperPage":
					document.title = "Archiving Helper";
					this.mainHeader.navigateToTab("TOOLS");
					this._showArchivingHelper();
					break;
				case "showUnarchivingHelperPage":
				    document.title = "Unarchiving Helper";
				    this.mainHeader.navigateToTab("TOOLS");
				    this._showUnarchivingHelper();
				    break;
				case "showUserManagerPage":
					document.title = "User Manager";
					this.mainHeader.navigateToTab("TOOLS");
					this._showUserManager();
					break;
                case "showUserManagementConfigPage":
                    document.title = "User Management Config";
                    this.mainHeader.navigateToTab("TOOLS");
                    this._showUserManagementConfig(arg);
                    break;
				case "showVocabularyManagerPage":
					document.title = "Vocabulary Browser";
					this.mainHeader.navigateToTab("TOOLS");
					this._showVocabularyManager();
					break;
				case "showTrashcanPage":
					document.title = "Trashcan";
					this.mainHeader.navigateToTab("TOOLS");
					this._showTrashcan();
					break;
				case "showStorageManager":
					document.title = "Storage Manager";
					this.mainHeader.navigateToTab("TOOLS");
					this._showStorageManager();
					break;
				case "showWelcomePage":
					document.title = "Main Menu";
					this.mainHeader.navigateToTab("LAB_NOTEBOOK");
					this._showWelcomePage();
					break;
				case "showBlancPage":
                    document.title = "Main Menu";
                    this.mainHeader.navigateToTab("LAB_NOTEBOOK");
                    this._showBlancPage();
                    break;
				case "showStockPage":
					document.title = "Stock";
					this.mainHeader.navigateToTab("LIMS");
					var newView = new StockController(this);
					var views = this._getNewViewModel(true, true, false, TabContentUtil.getTopLevelTabInfo("STOCK"));
					newView.init(views);
					this.currentView = newView;
					break;
				case "showSearchPage":
					document.title = "Search";
					var searchText = arg["searchText"];
					var searchDomain = arg["searchDomain"];
					var searchDomainLabel = arg["searchDomainLabel"];
					this.mainHeader.navigateToTab("TOOLS");
					this._showSearchPage(searchText, searchDomain, searchDomainLabel);
					break;
				case "showSpacePage":
					var _this = this;
					this.serverFacade.getSpaceFromCode(arg, function(space) {
						document.title = "Space " + space;
						_this.mainHeader.navigateToTabByEntity("SPACE", space, space);
						_this._showSpacePage(space);
					});
					break;
                case "showEditSpacePage":
                    var _this = this;
                    this.serverFacade.getSpaceFromCode(arg, function(space) {
                        document.title = "Space " + space;
                        _this._showEditSpacePage(space);
                    });
                    break;
				case "showProjectPageFromIdentifier":
					var _this = this;
					this.serverFacade.getProjectFromIdentifier(arg, function(project) {
						document.title = "Project " + project.code;
						_this.mainHeader.navigateToTabByEntity("PROJECT", project.spaceCode, project.permId);
						_this._showProjectPage(project);
					});
					break;
				case "showProjectPageFromPermId":
					var _this = this;
					this.serverFacade.getProjectFromPermId(arg, function(project) {
						document.title = "Project " + project.code;
						_this.mainHeader.navigateToTabByEntity("PROJECT", project.spaceCode, project.permId);
						_this._showProjectPage(project);
					});
					break;
				case "showEditProjectPageFromPermId":
					var _this = this;
					this.serverFacade.getProjectFromPermId(arg, function(project) {
						document.title = "Project " + project.code;
						_this._showEditProjectPage(project);
						//window.scrollTo(0,0);
					});
					break;
                case "showCreateSpacePage":
                    document.title = "Create Space";
                    this._showCreateSpacePage(arg);
                    break;
				case "showCreateProjectPage":
					document.title = "Create Project";
					this._showCreateProjectPage(arg);
					//window.scrollTo(0,0);
					break;
				case "showCreateExperimentPage":
					var experimentTypeCode = arg["experimentTypeCode"];
					var projectIdentifier = arg["projectIdentifier"];
					
                    document.title = "Create " + ELNDictionary.getExperimentKindName(experimentTypeCode) + " " + experimentTypeCode;
					var experiment = {
							experimentTypeCode : experimentTypeCode,
							identifier : projectIdentifier
					}
					this._showExperimentPage(experiment, FormMode.CREATE);
					//window.scrollTo(0,0);
					break;
				case "showExperimentPageFromPermId":
					var _this = this;
					var experimentRules = { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : arg } };
					var experimentCriteria = { entityKind : "EXPERIMENT", logicalOperator : "AND", rules : experimentRules };
					this.serverFacade.searchForExperimentsAdvanced(experimentCriteria, null, function(data) {
						_this.mainHeader.navigateToTabByEntity("COLLECTION", data);
						mainController.changeView('showExperimentPageFromIdentifier', encodeURIComponent('["' +
								data.objects[0].identifier.identifier + '",false]'));
					});
					break;
				case "showExperimentPageFromIdentifier":
					var _this = this;
					var argsArray = arg ? arg : [null, null];
					if(typeof(argsArray) === 'string') {
					    argsArray = [argsArray, false];
					}
					var spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(argsArray[0])
					_this.mainHeader.navigateToTabByEntity("COLLECTION", spaceCode);
					this._showExperimentView(argsArray[0], argsArray[1], "FORM_VIEW");
					break;
				case "showCreateDataSetPageFromExpPermId":
					var _this = this;
					var experimentRules = { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : arg } };
					var experimentCriteria = { entityKind : "EXPERIMENT", logicalOperator : "AND", rules : experimentRules };
					this.serverFacade.searchForExperimentsAdvanced(experimentCriteria, null, function(data) {
						document.title = "Create Data Set for " + data.objects[0].code;
						_this._showCreateDataSetPage(data.objects[0]);
						//window.scrollTo(0,0);
					});
					break;
				case "showEditExperimentPageFromIdentifier":
					var _this = this;
                    var argsArray = arg ? arg : [null, null];
                    var identifier = argsArray[0];
                    var type = argsArray[1];
                    this.serverFacade.listExperimentsForIdentifiers([identifier], function(data) {
                        document.title = "" + ELNDictionary.getExperimentKindName(type) + " " + identifier;
						_this._showExperimentPage(data.result[0], FormMode.EDIT);
						//window.scrollTo(0,0);
						_this.sideMenu.collapseSideMenu();
					});
					break;
				case "showCreateSamplePage":
				    var sampleTypeCode = arg["sampleTypeCode"];
				    var spaceCode = arg["spaceCode"];
				    var projectCode = arg["projectCode"];
				    var experimentIdentifier = arg["experimentIdentifier"];
				    document.title = "Create " + Util.getDisplayNameFromCode(sampleTypeCode);
				    this._showCreateSamplePage(sampleTypeCode, spaceCode, projectCode, experimentIdentifier);
                    break;
				case "showCreateSubExperimentPage":
					var sampleTypeCode = arg["sampleTypeCode"];
					var experimentIdentifier = arg["experimentIdentifier"];
					document.title = "Create " + Util.getDisplayNameFromCode(sampleTypeCode);
					this._showCreateSubExperimentPage(sampleTypeCode, experimentIdentifier);
					//window.scrollTo(0,0);
					break;
				case "showSamplesPage":
					document.title = "" + ELNDictionary.Sample + " Browser";
					var argsArray = arg ? arg : [null, null];
                    if(arg) {
                        var spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(argsArray[0])
                        this.mainHeader.navigateToTabByEntity("COLLECTION", spaceCode);
                    } else {
                        this.mainHeader.navigateToTab("TOOLS");
                    }
                    this._showExperimentView(argsArray[0], argsArray[1], "LIST_VIEW");
					break;
				case "showSampleHierarchyPage":
					document.title = "Hierarchy " + arg;
					this._showSampleHierarchyPage(arg);
					//window.scrollTo(0,0);
					break;
				case "showSampleHierarchyTablePage":
					document.title = "Table Hierarchy " + arg;
					this._showSampleHierarchyTablePage(arg);
					//window.scrollTo(0,0);
					break;
				case "showProjectHistoryPage":
					document.title = "History " + arg;
					this._showProjectHistoryPage(arg);
					break;
				case "showExperimentHistoryPage":
					document.title = "History " + arg;
					this._showExperimentHistoryPage(arg);
					break;
				case "showSampleHistoryPage":
					document.title = "History " + arg;
					this._showSampleHistoryPage(arg);
					break;
				case "showDatasetHistoryPage":
					document.title = "History " + arg;
					this._showDatasetHistoryPage(arg);
					break;
				case "showDatasetHierarchyTablePage":
					document.title = "Table Hierarchy " + arg;
					this._showDatasetHierarchyTablePage(arg);
					//window.scrollTo(0,0);
					break;
				case "showEditSamplePageFromPermId":
					var _this = this;
					var permId = null;
					var paginationInfo = null;
					if((typeof arg) !== "string") {
						permId = arg.permIdOrIdentifier;
						paginationInfo = arg.paginationInfo;
						arg = permId;
					} else {
						permId = arg;
					}
					this.serverFacade.searchWithUniqueId(permId, function(data) {
						if(!data[0]) {
							window.alert(refreshPageMessage);
						} else {
							document.title = "" + Util.getDisplayNameFromCode(data[0].sampleTypeCode) + " " + data[0].code;
							var isELNSubExperiment = $.inArray(data[0].spaceCode, _this.profile.inventorySpaces) === -1 && _this.profile.inventorySpaces.length > 0;
							_this._showEditSamplePage(data[0], isELNSubExperiment, paginationInfo);
							//window.scrollTo(0,0);
						}
					});
					break;
				case "showViewSamplePageFromPermId":
					var _this = this;
					var permId = null;
					var paginationInfo = null;
					var activeTab = null;
					if((typeof arg) !== "string") {
						permId = arg.permIdOrIdentifier;
						paginationInfo = arg.paginationInfo;
						activeTab = arg.activeTab;
						arg = permId;
					} else {
						permId = arg;
					}
					this.serverFacade.searchWithUniqueId(permId, function(data) {
						if(!data[0]) {
							window.alert(refreshPageMessage);
						} else {
							document.title = "" + Util.getDisplayNameFromCode(data[0].sampleTypeCode) + " " + data[0].code;
							var isELNSubExperiment = $.inArray(data[0].spaceCode, _this.profile.inventorySpaces) === -1 && _this.profile.inventorySpaces.length > 0;
							_this.mainHeader.navigateToTabByEntity("OBJECT", data[0].spaceCode, data[0].permId);
							_this._showViewSamplePage(data[0], isELNSubExperiment, paginationInfo, activeTab);
						}
					});
					break;
				case "showViewSamplePageFromIdentifier":
					var _this = this;
					var identifier = null;
					var paginationInfo = null;
					if((typeof arg) !== "string") {
						identifier = arg.permIdOrIdentifier;
						paginationInfo = arg.paginationInfo;
						arg = permId;
					} else {
						identifier = arg;
					}
					this.serverFacade.searchWithIdentifiers([identifier], function(data) {
						if(!data[0]) {
							window.alert(refreshPageMessage);
						} else {
							document.title = "" + Util.getDisplayNameFromCode(data[0].sampleTypeCode) + " " + data[0].code;
							var isELNSubExperiment = $.inArray(data[0].spaceCode, _this.profile.inventorySpaces) === -1&& _this.profile.inventorySpaces.length > 0;
							_this.mainHeader.navigateToTabByEntity("COLLECTION", data[0]);
							_this._showViewSamplePage(data[0], isELNSubExperiment, paginationInfo);
						}
					});
					break;
				case "showCreateDataSetPageFromPermId":
					var _this = this;
					this.serverFacade.searchWithUniqueId(arg, function(data) {
						if(!data[0]) {
							window.alert(refreshPageMessage);
						} else {
							document.title = "Create Data Set for " + data[0].code;
							_this._showCreateDataSetPage(data[0]);
						}
					});
					break;
				case "showViewDataSetPageFromPermId":
					var _this = this;
					var permId = null;
					var paginationInfo = null;
                    if((typeof arg) !== "string") {
                        permId = arg.permIdOrIdentifier;
                        paginationInfo = arg.paginationInfo;
                        arg = permId;
                    } else {
                        permId = arg;
                    }
					var dsCriteria = { 	
							entityKind : "DATASET", 
							logicalOperator : "AND", 
							rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
					};
					this.serverFacade.searchForDataSetsAdvanced(dsCriteria, {withPhysicalData: true}, function(results) {
						var datasetParentCodes = [];
						var dataset = results.objects[0];
						if(dataset) {
							for(var pIdx = 0; pIdx < dataset.parents.length; pIdx++) {
								datasetParentCodes.push(dataset.parents[pIdx].code);
							}
						}
						_this.serverFacade.searchDataSetWithUniqueId(arg, function(dataSetData) {
							if(!dataSetData.result || !dataSetData.result[0]) {
								window.alert(refreshPageMessage);
							} else {
								dataSetData.result[0].parentCodes = datasetParentCodes;
								if(dataSetData.result[0].sampleIdentifierOrNull) {
									_this.serverFacade.searchWithIdentifiers([dataSetData.result[0].sampleIdentifierOrNull], function(sampleData) {
										document.title = "Data Set " + dataSetData.result[0].code;
										var spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(dataSetData.result[0].sampleIdentifierOrNull)
                                        _this.mainHeader.navigateToTabByEntity("DATASET", spaceCode, dataset.code);
                                        _this._showViewDataSetPage(sampleData[0], dataSetData.result[0], dataset, paginationInfo);
									});
								} else if(dataSetData.result[0].experimentIdentifier) {
									_this.serverFacade.listExperimentsForIdentifiers([dataSetData.result[0].experimentIdentifier], function(experimentResults) {
										var experimentRules = { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : experimentResults.result[0].permId } };
										var experimentCriteria = { entityKind : "EXPERIMENT", logicalOperator : "AND", rules : experimentRules };
										_this.serverFacade.searchForExperimentsAdvanced(experimentCriteria, null, function(experimentData) {
											document.title = "Data Set " + dataSetData.result[0].code;
											var spaceCode = IdentifierUtil.getSpaceCodeFromIdentifier(dataSetData.result[0].experimentIdentifier)
                                            _this.mainHeader.navigateToTabByEntity("DATASET", spaceCode, dataset.code);
                                            _this._showViewDataSetPage(experimentData.objects[0], dataSetData.result[0], dataset, paginationInfo);
										});
									});
								}
							}
						});
					});
					break;
				case "showEditDataSetPageFromPermId":
					var _this = this;
					var dsCriteria = { 	
							entityKind : "DATASET", 
							logicalOperator : "AND", 
							rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : arg } }
					};
					
					this.serverFacade.searchForDataSetsAdvanced(dsCriteria, null, function(results) {
						var datasetParentCodes = [];
						var dataset = results.objects[0];
						if(dataset) {
							for(var pIdx = 0; pIdx < dataset.parents.length; pIdx++) {
								datasetParentCodes.push(dataset.parents[pIdx].code);
							}
						}
						_this.serverFacade.searchDataSetWithUniqueId(arg, function(dataSetData) {
							if(!dataSetData.result || !dataSetData.result[0]) {
								window.alert(refreshPageMessage);
							} else {
								dataSetData.result[0].parentCodes = datasetParentCodes;
								if(dataSetData.result[0].sampleIdentifierOrNull) {
									_this.serverFacade.searchWithIdentifiers([dataSetData.result[0].sampleIdentifierOrNull], function(sampleData) {
										document.title = "Data Set " + dataSetData.result[0].code;
										_this._showEditDataSetPage(sampleData[0], dataSetData.result[0], dataset);
										//window.scrollTo(0,0);
									});
								} else if(dataSetData.result[0].experimentIdentifier) {
									_this.serverFacade.listExperimentsForIdentifiers([dataSetData.result[0].experimentIdentifier], function(experimentResults) {
										var experimentRules = { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : experimentResults.result[0].permId } };
										var experimentCriteria = { entityKind : "EXPERIMENT", logicalOperator : "AND", rules : experimentRules };
										_this.serverFacade.searchForExperimentsAdvanced(experimentCriteria, null, function(experimentData) {
											document.title = "Data Set " + dataSetData.result[0].code;
											_this._showEditDataSetPage(experimentData.objects[0], dataSetData.result[0], dataset);
											//window.scrollTo(0,0);
										});
									});
								}
							}
						});
					});
					break;
				case "showDrawingBoard":
					var _this = this;
					document.title = "Drawing board";
					_this._showDrawingBoard();
					//window.scrollTo(0,0);
					break;
				case "showAbout":
				    this.mainHeader.navigateToTab("TOOLS");
					$.get('version.txt', function(data) {
						Util.showInfo("Current Version: " + data);
					}, 'text');
					break;
				case "EXTRA_PLUGIN_UTILITY":
				    var uniqueViewName = arg;
				    this.mainHeader.navigateToTab("TOOLS");
				    var viewContainers = mainController._getNewViewModel(true, true, false);
                    var pluginUtility = profile.getPluginUtility(uniqueViewName);
                    pluginUtility.paintView(viewContainers.header, viewContainers.content);
				    break
				default:
					window.alert("The system tried to create a non existing view: " + newViewChange);
					break;
			}
			mainController.tabContent.updateView(this.currentView)
		} catch(err) {
			Util.manageError(err);
		}

		
		//
		// Refresh Functionality
		//
		this.lastViewChange = newViewChange;
		this.lastArg = arg;
	}
	
	//
	// Functions that trigger view changes, should only be called from the main controller changeView method
	//
	this._getBackwardsCompatibleMainContainer = function(id, navigationTabInfo) {
	    var header = null;
		var content = $("<div>");
		content.css("padding", "10px");
		
		if(id) {
			content.attr("id", id);
		}

		var navigationTab = navigationTabInfo;
        if(!navigationTab) {
            navigationTab = TabContentUtil.getCleanTab(" ", false, id);
        }

        if(navigationTab) {
            let tabContentHeader = $("<div>", { id: "tab-content-header" });
            let tabContentBody = $("<div>", { id: "tab-content-body" });

            mainController.tabContent.openTab(navigationTab, function() {
                tab = $("#" + navigationTab.id)
                tab.empty()
                tab.append(tabContentHeader);
                tab.append(tabContentBody);
            });
            header = tabContentHeader;
            tabContentBody.append(content);
            content = tabContentBody;
        }
		
		this.views.header = null;
		this.views.content = content;
		this.views.auxContent = null;
		LayoutManager.reloadView(this.views);
		
		return content;
	}
	
	this._getNewViewModel = function(withHeaderOrHeaderId, withContentOrContentId, withAuxContentOrAuxContentId, navigationTabInfo, callback) {
		var header = null;
		var content = null;
		var auxContent = null;
		var tab = null;

		var navigationTab = navigationTabInfo;
		if(!navigationTab) {
		    navigationTab = TabContentUtil.getCleanTab();
		}

		if(navigationTab) {
		    let tabContentHeader = $("<div>", { id: "tab-content-header" });
		    let tabContentBody = $("<div>", { id: "tab-content-body" });

            mainController.tabContent.openTab(navigationTab, function() {
                tab = $("#" + navigationTab.id)
                tab.empty()
                tab.append(tabContentHeader);
                tab.append(tabContentBody);
                if(callback) {
                    callback();
                }
            });
            header = tabContentHeader;
            content = tabContentBody;
        }

		if(withHeaderOrHeaderId) {
		    if(!header) {
			    header = $("<div>");
			}
			header.css({ 
				"padding" : "10px",
				"height" : "100%",
				"background-color" : "rgb(248, 248, 248)",
				"position" : "relative",
                "z-index" : 500
			});
			
			if((typeof withHeaderOrHeaderId === 'string' || withHeaderOrHeaderId instanceof String)) {
				header.attr("id", withHeaderOrHeaderId);
			}
		}
		
		if(withContentOrContentId) {
		    if(!content) {
			    content = $("<div>");
			}
			content.css({ 
				"padding" : "10px"
			});
			if(!withAuxContentOrAuxContentId) {  // Setting 100% height breaks views with 3 columns on tablet mode, better to explicitly set to 100% when the third column is not used
				content.css({
					"height" : "100%"
				});
			}
			if((typeof withContentOrContentId === 'string' || withContentOrContentId instanceof String)) {
				content.attr("id", withContentOrContentId);
			}
		}
		
		if(withAuxContentOrAuxContentId) {
			auxContent = $("<div>");
			if((typeof withContentAuxOrContentAuxId === 'string' || withAuxContentOrAuxContentId instanceof String)) {
				auxContent.attr("id", withAuxContentOrAuxContentId);
			}
		}
		
		this.views.header = header;
		this.views.content = content;
		this.views.auxContent = auxContent;
		
		LayoutManager.reloadView(this.views);
		
		var modificableViews = {
				header : this.views.header,
				content : this.views.content,
				auxContent : this.views.auxContent,
				tabId: navigationTab.id
		};
		
		return modificableViews;
	}

	this._showUserProfilePage = function(mode) {
		var newView = new UserProfileController(this, mode);
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('USER_PROFILE'));
		newView.init(views);
		this.currentView = newView;
	}

	this._showSettingsPage = function(mode, sampleIdentifier) {
		if(!sampleIdentifier) {
			this._selectSettings();
		} else {
			var _this = this;
			this.serverFacade.searchSamples({ "sampleIdentifier" : sampleIdentifier, "withProperties" : true }, function(data) {
				if(!data[0]) {
					window.alert("Settings object doesn't exist, settings can't be edited, this is not supposed to happen, contact your admin.");
				} else {
					var newView = new SettingsFormController(_this, data[0], mode);
					var tabInfo = TabContentUtil.getToolTabInfo('SETTINGS');
					tabInfo.changed = mode === FormMode.EDIT;
					var views = _this._getNewViewModel(true, true, false, tabInfo);
					newView.init(views);
					newView.tabId = tabInfo.id;
					_this.currentView = newView;
					if(mode === FormMode.EDIT) {
					    _this.sideMenu.collapseSideMenu();
					}
				}
			});
		}
	}
	
    this._showCustomImportPage = function() {
        var newView = new CustomImportController(this);
        var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('IMPORT'));
        newView.init(views);
    }

    this._showOtherToolsPage = function() {
        var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('OTHER_TOOLS'));
        var $header = views.header;
        $header.append($("<h1>").append("Other Tools"));
        var $diskSpaceButton = FormUtil.getButtonWithIcon("glyphicon-hdd", function () {
            FormUtil.showDiskSpaceDialog();
        }, "Show available storage space", null, null, 'btn btn-primary btn-secondary');
        $header.append(FormUtil.getToolbar([{ component : $diskSpaceButton }]));
        var $downloadMetadataTemplateButton = FormUtil.getButtonWithIcon("glyphicon-export", function () {
            FormUtil.downloadMetadataTemplateDialog();
        }, "Download metadata.json template", null, null, 'btn btn-primary btn-secondary');
        $header.append(FormUtil.getToolbar([{ component : $downloadMetadataTemplateButton }]));
        if (profile.extraToolActions) {
            profile.extraToolActions.forEach(function(actionDefinition) {
                var $actionButton = FormUtil.getButtonWithText(actionDefinition.label, actionDefinition.action);
                $header.append(FormUtil.getToolbar([{ component : $actionButton }]));
            });
        }
    }

    this.createReport = function(reportDefinition) {
        var parameters = reportDefinition.parameters ? reportDefinition.parameters : {};
        mainController.serverFacade.customASService(parameters, function(result){
            var mimeType = reportDefinition.mimeType ? reportDefinition.mimeType : "text/plain";
            Util.download(result, mimeType, reportDefinition.binary, reportDefinition.filename);
        }, reportDefinition.service);
    }

	
	this._selectSettings = function() {
		this.serverFacade.searchSamples({ 	"sampleTypeCode" : "GENERAL_ELN_SETTINGS",
											"withProperties" : false }, (function(settingsObjects) {
			if(settingsObjects && settingsObjects.length === 1 && settingsObjects[0].identifier === "/ELN_SETTINGS/GENERAL_ELN_SETTINGS") {
                Util.unblockUI();
                mainController.changeView("showSettingsPage", settingsObjects[0].identifier);
			} else if(settingsObjects && settingsObjects.length > 0) {
				settingsObjects.sort(function(a, b) {
				    if(a.identifier === "/ELN_SETTINGS/GENERAL_ELN_SETTINGS") { // Global settings are first on the list
				    		return 1;
				    } else {
				    		return -1;
				    }
				});
				
				var settingsForDropdown = [];
				for(var sIdx = 0; sIdx < settingsObjects.length; sIdx++) {
				    var groupName = Util.getDisplayNameFromCode(SettingsManagerUtils.getSpaceGroupPrefix(settingsObjects[sIdx].spaceCode));
				    if(settingsObjects[sIdx].identifier === "/ELN_SETTINGS/GENERAL_ELN_SETTINGS") {
				        groupName = "General ELN Settings";
				    }
				    settingsForDropdown.push({ label: groupName, value: settingsObjects[sIdx].identifier})
				}
				
				var $dropdown = FormUtil.getDropdown(settingsForDropdown, "Select Group Settings");
				$dropdown.attr("id", "settingsDropdown");
				Util.showDropdownAndBlockUI("settingsDropdown", $dropdown, "Group settings only apply to group spaces.");
				
				$("#settingsDropdown").on("change", function(event) {
					var sampleIdentifier = $("#settingsDropdown")[0].value;
					Util.unblockUI();
					mainController.changeView("showSettingsPage", sampleIdentifier);
				});
				
				$("#settingsDropdownCancel").on("click", function(event) { 
					Util.unblockUI();
				});
		
			} else {
				window.alert("Settings object doesn't exist, settings can't be edited, this is not supposed to happen, contact your admin.");
			}
		}).bind(this))
		
		
	}
	
	this._showStorageManager = function() {
	    var tabInfo = TabContentUtil.getToolTabInfo('STORAGE_MANAGER');
		var views = this._getNewViewModel(true, true, false, tabInfo);
		var storageManagerController = new StorageManagerController(this);
		storageManagerController.init(views);
		storageManagerController.tabId = tabInfo.id;
		this.currentView = storageManagerController;
	}
	
	this._showVocabularyManager = function() {
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('VOCABULARY_BROWSER'));
		
		var vocabularyManagerController = new VocabularyManagerController(this);
		vocabularyManagerController.init(views);
		this.currentView = vocabularyManagerController;
	}
	
	this._showWelcomePage = function() {
		var content = this._getBackwardsCompatibleMainContainer();

		$.ajax({ cache: false,
        		 url: "./etc/welcome.html",
        		 success: function (data) {
        			content.html(data);
        		 },
        		 settings: { suppressErrors: true },
        		 error: function (xhr, ajaxOptions, thrownError){
        		    	console.log("AJAX Error status: " + xhr.status + " - Status text: " + xhr.statusText + " - Using default text.");
        				content.html('<p>Welcome to openBIS ELN-LIMS</p>');
        		 }
        });

		this.currentView = {
		    content : content
		}
	}

	this._showBlancPage = function() {
    		var content = this._getBackwardsCompatibleMainContainer();
    		content.html('');
    		this.currentView = {
    		    content : content
    		}
    	}
	
	this._showDrawingBoard = function() {
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('DRAWING_BOARD'));
		
		var drawingBoardsController = new DrawingBoardsController(this);
		drawingBoardsController.init(views);
		this.currentView = drawingBoardsController;
	}
	
	this._showUserManager = function() {
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('USER_MANAGER'));
		
		var userManagerController = new UserManagerController(this);
		userManagerController.init(views);
		this.currentView = userManagerController;
	}

	this._showUserManagementConfig = function(arg) {
        var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('USER_MANAGEMENT_CONFIG'));
        
        var userManagementConfigController = new UserManagementConfigController(this, arg);
        userManagementConfigController.init(views);
        this.currentView = userManagementConfigController;
    }

    this._showDropboxMonitor = function() {
        var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('DROPBOX_MONITOR'));
        var dropboxMonitorController = new DropboxMonitorController(this);
        dropboxMonitorController.init(views);
        this.currentView = dropboxMonitorController;
    }
    
    this._showArchivingHelper = function() {
        var tabInfo = TabContentUtil.getToolTabInfo('ARCHIVING_HELPER');
        var views = this._getNewViewModel(true, true, false, tabInfo);
        
        var archivingHelperController = new ArchivingHelperController(this);
        archivingHelperController.init(views);
        archivingHelperController.tabId = tabInfo.id;
        this.currentView = archivingHelperController;
    }
    
	this._showUnarchivingHelper = function() {
	    var tabInfo = TabContentUtil.getToolTabInfo('UNARCHIVING_HELPER');
		var views = this._getNewViewModel(true, true, false, tabInfo);
		
		var unarchivingHelperController = new UnarchivingHelperController(this);
		unarchivingHelperController.init(views);
		unarchivingHelperController.tabId = tabInfo.id;
		this.currentView = unarchivingHelperController;
	}
	
	this._showExperimentView = function(experimentIdentifier, forced, forcedView) {
        var _this = this;
		if (experimentIdentifier) {
			this.serverFacade.listExperimentsForIdentifiers([experimentIdentifier], function (data) {
				var experiment = data.result[0];
				var defaultCollectionView = experiment.properties["DEFAULT_COLLECTION_VIEW"];
				var collectionView = forced || !defaultCollectionView ? forcedView : defaultCollectionView;

				switch (collectionView) {
					case "LIST_VIEW": {
						if (experimentIdentifier === "null") { //Fix for reloads when there is text on the url
							experimentIdentifier = null;
						}
						var tabInfo = TabContentUtil.getExperimentTabInfo(experiment, FormMode.VIEW);
                        var views = _this._getNewViewModel(true, true, false, tabInfo);
						var sampleTableController = new SampleTableController(_this,
                            Util.getDisplayNameFromCode(experiment.experimentTypeCode) + " " +
							experimentIdentifier, experimentIdentifier, null, null, experiment);
						sampleTableController.init(views);
						sampleTableController.tabId = tabInfo.id;
						_this.currentView = sampleTableController;
						mainController.tabContent.updateView(_this.currentView)
						break;
					}
					case "FORM_VIEW":
					default:{
						document.title = ELNDictionary.getExperimentKindName(experiment.experimentTypeCode) + " " +
							experimentIdentifier;
						_this._showExperimentPage(experiment, FormMode.VIEW);
						break;
					}
				}
			});
		} else {
		var tabInfo = TabContentUtil.getToolTabInfo("OBJECT_BROWSER");
		    var views = this._getNewViewModel(true, true, false, tabInfo);
			sampleTableController = new SampleTableController(_this, "" + ELNDictionary.Sample +
				" Browser", null, null);
			sampleTableController.tabId = tabInfo.id;
			sampleTableController.init(views);
			_this.currentView = sampleTableController;
		}
	}

	this._showSampleHierarchyPage = function(permId) {
		//Show View
		var localInstance = this;
		this.serverFacade.searchWithUniqueIdCompleteTree(permId, function(data) {
		    var sample = data[0];
		    var initialized = false;
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getSampleTabInfo(sample, FormMode.VIEW, "GRAPH"), () => {initialized=true} );
			var sampleHierarchy = new SampleHierarchy(localInstance.serverFacade, views, localInstance.profile, sample);

	        var counter = 50;
	        var repeatUntilSet = function() {
                if(initialized || counter <= 0) {
                    sampleHierarchy.init();
                    localInstance.currentView = sampleHierarchy;
                    mainController.tabContent.updateView(sampleHierarchy)
                } else {
                    counter--;
                   setTimeout(repeatUntilSet, 50);
                }
            }
            repeatUntilSet();

		});
	}
	
	this._showSampleHierarchyTablePage = function(permId) {
		//Show View
		var localInstance = this;
		
		var sCriteria = { 	
			entityKind : "SAMPLE", 
			logicalOperator : "AND", 
			rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
		};
					
		this.serverFacade.searchForSamplesAdvanced(sCriteria, null, function(results) {
			var sample = results.objects[0];
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getSampleTabInfo(sample, FormMode.VIEW, "TABLE"));
			var hierarchyTableController = new HierarchyTableController(this, sample);
			hierarchyTableController.init(views);
			localInstance.currentView = hierarchyTableController;
			mainController.tabContent.updateView(hierarchyTableController)
		});
	}

	this._showProjectHistoryPage = function(permId) {
		var localInstance = this;
		
		var criteria = {
			entityKind : "PROJECT", 
			logicalOperator : "AND", 
			rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
		};

		var fetchOptions = { only : true, withHistory: { withAuthor: true } }

		this.serverFacade.searchForProjectsAdvanced(criteria, fetchOptions, function(results) {
			var project = results.objects[0];
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getProjectTabInfo(project, FormMode.VIEW, "HISTORY"));
			var historyController = new HistoryController(this, project);
			historyController.init(views);
			localInstance.currentView = historyController;
		});
	}


	this._showExperimentHistoryPage = function(permId) {
		var localInstance = this;
		
		var criteria = {
			entityKind : "EXPERIMENT", 
			logicalOperator : "AND", 
			rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
		};

		var fetchOptions = { only : true, withType: { withPropertyTypes: true }, withHistory: { withAuthor: true }, withProperties: true }

		this.serverFacade.searchForExperimentsAdvanced(criteria, fetchOptions, function(results) {
			var experiment = results.objects[0];
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getExperimentTabInfo(experiment, FormMode.VIEW, "HISTORY"));
			var historyController = new HistoryController(this, experiment);
			historyController.init(views);
			localInstance.currentView = historyController;
		});
	}

	this._showSampleHistoryPage = function(permId) {
		var localInstance = this;
		
		var criteria = {
			entityKind : "SAMPLE", 
			logicalOperator : "AND", 
			rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
		};

		var fetchOptions = { only : true, withType: { withPropertyTypes: true }, withHistory: { withAuthor: true }, withProperties: true }

		this.serverFacade.searchForSamplesAdvanced(criteria, fetchOptions, function(results) {
			var sample = results.objects[0];
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getSampleTabInfo(sample, FormMode.VIEW, "HISTORY"));
			var historyController = new HistoryController(this, sample);
			historyController.init(views);
			localInstance.currentView = historyController;
		});
	}

	this._showDatasetHistoryPage = function(permId) {
		var localInstance = this;
		
		var criteria = {
			entityKind : "DATASET", 
			logicalOperator : "AND", 
			rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
		};

		var fetchOptions = { only : true, withType: { withPropertyTypes: true }, withHistory: { withAuthor: true }, withProperties: true }

		this.serverFacade.searchForDataSetsAdvanced(criteria, fetchOptions, function(results) {
			var dataset = results.objects[0];
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getDataSetTabInfo(dataset, FormMode.VIEW, "HISTORY"));
			var historyController = new HistoryController(this, dataset);
			historyController.init(views);
			localInstance.currentView = historyController;
		});
	}
	
	this._showDatasetHierarchyTablePage = function(permId) {
		//Show View
		var localInstance = this;
		
		var dsCriteria = { 	
			entityKind : "DATASET", 
			logicalOperator : "AND", 
			rules : { "UUIDv4" : { type : "Attribute", name : "PERM_ID", value : permId } }
		};
					
		this.serverFacade.searchForDataSetsAdvanced(dsCriteria, null, function(results) {
			var dataset = results.objects[0];
			var views = localInstance._getNewViewModel(true, true, false, TabContentUtil.getDataSetTabInfo(dataset, FormMode.VIEW, "TABLE"));
			var hierarchyTableController = new HierarchyTableController(this, dataset);
			hierarchyTableController.init(views);
			localInstance.currentView = hierarchyTableController;
			mainController.tabContent.updateView(hierarchyTableController)
		});
	}

	this.openHelpPage = function() {
        var src = "https://openbis.readthedocs.io/en/20.10.x/user-documentation/general-users";
        var win = window.open(src, '_blank');
        win.focus();
	}

	this._showCreateSamplePage = function(sampleTypeCode, spaceCode, projectCode, experimentIdentifier) {
	    //Update menu
        var sampleTypeDisplayName = this.profile.getSampleTypeForSampleTypeCode(sampleTypeCode).description;
        if(sampleTypeDisplayName === null) {
            sampleTypeDisplayName = sampleTypeCode;
        }

        var sample = {
            sampleTypeCode : sampleTypeCode,
            spaceCode: spaceCode,
            projectCode: projectCode,
            experimentIdentifierOrNull: experimentIdentifier,
            properties : {}
        };

        var sampleFormController = new SampleFormController(this, FormMode.CREATE, sample);
        this.currentView = sampleFormController;
        var tabInfo = TabContentUtil.getCleanTab("CREATE_SAMPLE", true);
        var views = this._getNewViewModel(true, true, false, tabInfo);
        sampleFormController.init(views);
        sampleFormController.tabId = tabInfo.id;
        this.sideMenu.collapseSideMenu();
	}
	
	this._showCreateSubExperimentPage = function(sampleTypeCode, experimentIdentifier) {
		//Update menu
		var sampleTypeDisplayName = this.profile.getSampleTypeForSampleTypeCode(sampleTypeCode).description;
		if(sampleTypeDisplayName === null) {
			sampleTypeDisplayName = sampleTypeCode;
		}
		
		//Show Form
		var sample = {
				sampleTypeCode : sampleTypeCode,
				spaceCode : IdentifierUtil.getSpaceCodeFromIdentifier(experimentIdentifier),
				projectCode : IdentifierUtil.getProjectCodeFromExperimentIdentifier(experimentIdentifier),
				experimentIdentifierOrNull : experimentIdentifier,
				properties : {}
		}
		var sampleFormController = new SampleFormController(this, FormMode.CREATE, sample);
		this.currentView = sampleFormController;
		var tabInfo = TabContentUtil.getCleanTab("CREATE_SAMPLE", true);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		sampleFormController.init(views);
		sampleFormController.tabId = tabInfo.id;
		this.sideMenu.collapseSideMenu();
	}
	
	this._showTrashcan = function() {
		var trashcanController = new TrashManagerController(this);
		this.trashcanController = trashcanController;
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getToolTabInfo('TRASHCAN'));
		trashcanController.init(views);
	}
	
	this._showViewSamplePage = function(sample, isELNSubExperiment, paginationInfo, activeTab) {
		//Show Form
		var sampleFormController = new SampleFormController(this, FormMode.VIEW, sample, paginationInfo, activeTab);
		var tabInfo = TabContentUtil.getSampleTabInfo(sample, FormMode.VIEW);
		var views = this._getNewViewModel(true, true, true, tabInfo);
		sampleFormController.init(views);
		sampleFormController.tabId = tabInfo.id;
		this.currentView = sampleFormController;
		mainController.tabContent.updateView(this.currentView)
	}
	
	this._showEditSamplePage = function(sample, isELNSubExperiment, paginationInfo) {
		//Show Form
		var localInstance = this;
		this.serverFacade.searchWithUniqueId(sample.permId, function(data) {
			var sampleFormController = new SampleFormController(localInstance, FormMode.EDIT, data[0], paginationInfo);
			localInstance.currentView = sampleFormController;
			var tabInfo = TabContentUtil.getSampleTabInfo(sample, FormMode.EDIT)
			var views = localInstance._getNewViewModel(true, true, false, tabInfo);
			sampleFormController.init(views);
			sampleFormController.tabId = tabInfo.id;
			localInstance.sideMenu.collapseSideMenu();
		});
	}
	
    this._showCreateSpacePage = function(isInventory) {
        //Show Form
        var spaceFormController = new SpaceFormController(this, FormMode.CREATE, isInventory);
        var tabInfo = TabContentUtil.getCleanTab("CREATE_SPACE", true);
        var views = this._getNewViewModel(true, true, false, tabInfo);
        spaceFormController.init(views);
        spaceFormController.tabId = tabInfo.id;
        this.currentView = spaceFormController;
        this.sideMenu.collapseSideMenu();
    }

	this._showSpacePage = function(space) {
	    var isRegularSpace = (SettingsManagerUtils.isInventorySpace(space) || SettingsManagerUtils.isStockSpace(space));
		//Show Form
		var spaceFormController = new SpaceFormController(this, FormMode.VIEW, isRegularSpace, space);
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getSpaceTabInfo(space, FormMode.VIEW));
		spaceFormController.init(views);
		this.currentView = spaceFormController;
		this.currentView.tabId = views.tabId;
        mainController.tabContent.updateView(this.currentView)
	}
	
    this._showEditSpacePage = function(space) {
        //Show Form
        var spaceFormController = new SpaceFormController(this, FormMode.EDIT, false, space);
        var tabInfo = TabContentUtil.getSpaceTabInfo(space, FormMode.EDIT);
        var views = this._getNewViewModel(true, true, false, tabInfo);
        spaceFormController.init(views);
        spaceFormController.tabId = tabInfo.id;
        this.currentView = spaceFormController;
        this.sideMenu.collapseSideMenu();
    }
    
	this._showCreateProjectPage = function(spaceCode) {
		//Show Form
		var projectFormController = new ProjectFormController(this, FormMode.CREATE, {spaceCode : spaceCode});
		var tabInfo = TabContentUtil.getCleanTab("CREATE_PROJECT", true);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		projectFormController.init(views);
		projectFormController.tabId = tabInfo.id;
		this.currentView = projectFormController;
		this.sideMenu.collapseSideMenu();
	}
	
	this._showProjectPage = function(project) {
		//Show Form
		var projectFormController = new ProjectFormController(this, FormMode.VIEW, project);
		var tabInfo = TabContentUtil.getProjectTabInfo(project, FormMode.VIEW);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		projectFormController.init(views);
		projectFormController.tabId = tabInfo.id;
		this.currentView = projectFormController;
		mainController.tabContent.updateView(this.currentView)
	}
	
	this._showEditProjectPage = function(project) {
		//Show Form
		var projectFormController = new ProjectFormController(this, FormMode.EDIT, project);
		var tabInfo = TabContentUtil.getProjectTabInfo(project, FormMode.EDIT);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		projectFormController.init(views);
		projectFormController.tabId = tabInfo.id;
		this.currentView = projectFormController;
		this.sideMenu.collapseSideMenu();
	}
	
	this._showExperimentPage = function(experiment, mode) {
		//Show Form
		var experimentFormController = new ExperimentFormController(this, mode, experiment);
        var tabInfo = TabContentUtil.getExperimentTabInfo(experiment, mode);
		var views = this._getNewViewModel(true, true, undefined, tabInfo);
		experimentFormController.init(views);
		this.currentView = experimentFormController;
		this.currentView.tabId = tabInfo.id;
		mainController.tabContent.updateView(this.currentView)
		if(mode !== FormMode.VIEW) {
		    this.sideMenu.collapseSideMenu();
		}
	}
	
	this._showCreateDataSetPage = function(entity) {
		//Show Form
		var newView = new DataSetFormController(this, FormMode.CREATE, entity, null);
		var tabInfo = TabContentUtil.getDataSetTabInfo(null, FormMode.CREATE);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		newView.init(views);
		newView.tabId = tabInfo.id;
		this.currentView = newView;
		this.sideMenu.collapseSideMenu();
	}
	
	this._showViewDataSetPage = function(sampleOrExperiment, dataset, datasetV3, paginationInfo) {
		//Show Form
		var newView = new DataSetFormController(this, FormMode.VIEW, sampleOrExperiment, dataset, null, datasetV3, paginationInfo);
		var views = this._getNewViewModel(true, true, false, TabContentUtil.getDataSetTabInfo(dataset, FormMode.VIEW));
		newView.init(views);
		this.currentView = newView;
	}
	
	this._showEditDataSetPage = function(sampleOrExperiment, dataset, datasetV3) {
		//Show Form
		var newView = new DataSetFormController(this, FormMode.EDIT, sampleOrExperiment, dataset, null, datasetV3);
		var tabInfo = TabContentUtil.getDataSetTabInfo(dataset, FormMode.EDIT);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		newView.init(views);
		newView.tabId = tabInfo.id;
		this.currentView = newView;
		this.sideMenu.collapseSideMenu();
	}
	
	this._showAdvancedSearchPage = function(freeText) {
		//Show Form
		var newView = null;
		
		if(freeText) {
			$("#search").addClass("search-query-searching");
			newView = new AdvancedSearchController(this, freeText);
		} else {
			newView = new AdvancedSearchController(this);
		}
		var tabInfo = TabContentUtil.getToolTabInfo('SEARCH', freeText);
		var views = this._getNewViewModel(true, true, false, tabInfo);
		newView.init(views);
		newView.tabId = tabInfo.id;
		if(freeText) {
		    setTimeout(function(){ newView.search(); }, 1000);
		}
		this.currentView = newView;
	}
	
	this.lastSearchId = 0; //Used to discard search responses that don't pertain to the last search call.
	
	this._showSearchPage = function(value, searchDomain, searchDomainLabel) {
		this.lastSearchId++;
		var localSearchId = this.lastSearchId;
		var localReference = this;
		
		if(value.length === 0) {
			return;
		}
		
		var possibleSearch = function(localSearchId) {
			return function() {
				if(localSearchId === localReference.lastSearchId) { //Trigger it if no new have started
					
					if(value.length < 1) {
						return;
					}
					
					$("#search").addClass("search-query-searching");
					if(!searchDomain || searchDomain === profile.getSearchDomains()[0].name) { //Global Search
						if(profile.searchSamplesUsingV3OnDropbox) {
							localReference._legacyGlobalSearch(value);
						} else {
							$("#search").removeClass("search-query-searching");
							localReference.changeView("showAdvancedSearchPage", value);
						}
					} else if(searchDomain == "data-set-file-search") { 
						localReference.serverFacade.searchOnSearchDomain(searchDomain, value, function(data) {
							
							if(localSearchId === localReference.lastSearchId) {
								$("#search").removeClass("search-query-searching");
								
								var columns = [ {
									label : 'Entity Kind',
									property : 'entityKind',
									sortable : true
								}, {
									label : 'Entity Type',
									property : 'entityType',
									sortable : true
								}, {
									label : 'Code',
									property : 'code',
									sortable : true
								}, {
									label : 'Path',
									property : 'pathInDataSet',
									sortable : true
								}];
								
								var getDataList = function(callback) {
									var dataList = [];
									if(data.result) {
										for(var i = 0; i < data.result.length; i++) {
											var result = data.result[i];
											
											dataList.push({
												id : result.resultLocation.permId,
												entityKind : result.resultLocation.entityKind,
												entityType : result.resultLocation.entityType,
												permId : result.resultLocation.permId,
												code : result.resultLocation.code,
												pathInDataSet : result.resultLocation.pathInDataSet
											});
										}
									}
									callback(dataList);
								};
								
								var rowClick = function(e) {
									switch(e.data.entityKind) {
										case "DATA_SET":
											mainController.changeView('showViewDataSetPageFromPermId', e.data.permId);
											break;
									}
								}
								
								var dataGrid = new DataGridController(searchDomainLabel + " Search Results", columns, [], null, getDataList, rowClick, true, "SEARCH_" + searchDomainLabel, false, {
									fileFormat: DataGridExportOptions.FILE_FORMAT.TSV,
									filePrefix: 'search-' + searchDomainLabel
								}, 90);
								localReference.currentView = dataGrid;
								var content = localReference._getBackwardsCompatibleMainContainer();
								dataGrid.init(content);
								history.pushState({id: ++this._stateCounter}, "", ""); //History Push State
							}
						});
					} else {
						localReference.serverFacade.searchOnSearchDomain(searchDomain, value, function(data) {
							var dataSetCodes = [];
							for(var i = 0; i < data.result.length; i++) {
								var result = data.result[i];
								var resultLocation = result.resultLocation;
								if(resultLocation.entityKind === "DATA_SET") {
									dataSetCodes.push(resultLocation.code);
								}
							}
							
							localReference.serverFacade.getSamplesForDataSets(dataSetCodes, function(samplesData) {
								var getSampleIdentifierForDataSetCode = function(dataSetCode) {
									for(var i = 0; i < samplesData.result.length; i++) {
										if(samplesData.result[i].code === dataSetCode) {
											return samplesData.result[i].sampleIdentifierOrNull;
										}
									}
									return null;
								}
								
								if(localSearchId === localReference.lastSearchId) {
									$("#search").removeClass("search-query-searching");
									
									var columns = [ {
										label : 'Entity Kind',
										property : 'kind',
										sortable : true
									}, {
										label : 'Code',
										property : 'code',
										sortable : true
									}, {
										label : 'Found in',
										property : 'location',
										sortable : true
									}, {
										label : 'Sequence (Start - End)',
										property : 'sequenceStartEnd',
										sortable : true
									}, {
										label : 'Query (Start - End)',
										property : 'queryStartEnd',
										sortable : true
									}, {
										label : 'No. Mismatches',
										property : 'numberOfMismatches',
										sortable : true
									}, {
										label : 'No. Gaps',
										property : 'totalNumberOfGaps',
										sortable : true
									}, {
										label : 'E-value',
										property : 'evalue',
										sortable : true
									}, {
										label : 'Score',
										property : 'score',
										sortable : true
									}, {
										label : 'Bit Score',
										property : 'bitScore',
										sortable : true
									}];
									
									var getDataList = function(callback) {
										var dataList = [];
										if(data.result) {
											for(var i = 0; i < data.result.length; i++) {
												var result = data.result[i];
												var resultLocation = result.resultLocation;
												
												var code = resultLocation.code;
												var numberOfMismatches = resultLocation.alignmentMatch.numberOfMismatches;
												var totalNumberOfGaps = resultLocation.alignmentMatch.totalNumberOfGaps;
												var sequenceStartEnd = resultLocation.alignmentMatch.sequenceStart + "-" + resultLocation.alignmentMatch.sequenceEnd;
												var queryStartEnd = resultLocation.alignmentMatch.queryStart + "-" + resultLocation.alignmentMatch.queryEnd;
												var location = null;
												
												if(resultLocation.propertyType) {
													location = "Property: " + resultLocation.propertyType;
												} else if(resultLocation.pathInDataSet) {
													location = "Path: " + resultLocation.pathInDataSet;
												}
												
												if(resultLocation.entityKind === "DATA_SET") {
													code += "<br> " + ELNDictionary.Sample + ": " + getSampleIdentifierForDataSetCode(resultLocation.code);
												}
												
												dataList.push({
													id : resultLocation.permId,
													kind : resultLocation.entityKind,
													code : code,
													permId : resultLocation.permId,
													score : result.score.score,
													bitScore : result.score.bitScore,
													evalue : result.score.evalue,
													numberOfMismatches : numberOfMismatches,
													totalNumberOfGaps : totalNumberOfGaps,
													location : location,
													sequenceStartEnd : sequenceStartEnd,
													queryStartEnd : queryStartEnd
												});
											}
										}
										
										callback(dataList);
									};
									
									var rowClick = function(e) {
										switch(e.data.kind) {
											case "SAMPLE":
												mainController.changeView('showViewSamplePageFromPermId', e.data.permId);
												break;
											case "DATA_SET":
												mainController.changeView('showViewDataSetPageFromPermId', e.data.permId);
												break;
										}
									}
									
									var dataGrid = new DataGridController(searchDomainLabel + " Search Results", columns, [], null, getDataList, rowClick, true, "SEARCH_" + searchDomainLabel, false, {
										fileFormat: DataGridExportOptions.FILE_FORMAT.TSV,
										filePrefix: 'search-' + searchDomainLabel
									}, 90);
									localReference.currentView = dataGrid;
									var content = localReference._getBackwardsCompatibleMainContainer();
									dataGrid.init(content);
									history.pushState({id: ++this._stateCounter}, "", ""); //History Push State
								} else {
									//Discard old response, was triggered but a new one was started
								}
							});
						});
					}
					
				} else {
					//Discard it
				}
			}
		}
		
		setTimeout(possibleSearch(localSearchId), 800);
	}
	
	this._legacyGlobalSearch = function(value) {
		var localReference = this;
		localReference.serverFacade.searchWithText(value, function(data) {
			$("#search").removeClass("search-query-searching");
			
			var columns = [ {
				label : 'Code',
				property : 'code',
				sortable : true
			}, {
				label : 'Score',
				property : 'score',
				sortable : true
			}, {
				label : 'Preview',
				property : 'preview',
				sortable : false,
				render : function(data) {
					var previewContainer = $("<div>");
					mainController.serverFacade.searchDataSetsWithTypeForSamples("ELN_PREVIEW", [data.permId], function(data) {
						data.result.forEach(function(dataset) {
							var listFilesForDataSetCallback = function(dataFiles) {
								var downloadUrl = profile.allDataStores[0].downloadUrl + '/' + dataset.code + "/" + dataFiles.result[1].pathInDataSet + "?sessionID=" + mainController.serverFacade.getSession();
								var previewImage = $("<img>", { 'src' : downloadUrl, 'class' : 'zoomableImage', 'style' : 'height:80px;' });
								previewImage.click(function(event) {
									Util.showImage(downloadUrl);
									event.stopPropagation();
								});
								previewContainer.append(previewImage);
							};
							mainController.serverFacade.listFilesForDataSet(dataset.code, "/", true, listFilesForDataSetCallback);
						});
					});
					return previewContainer;
				},
				filter : function(data, filter) {
					return false;
				},
				sort : function(data1, data2, asc) {
					return 0;
				}
			}, {
				label : 'Sample Type',
				property : 'sampleTypeCode',
				sortable : true
			}, {
				label : 'Matched',
				property : 'matched',
				sortable : true,
				filter : function(data, filter) {
					var matchedValue = data.matched.text();
					return matchedValue.toLowerCase().indexOf(filter) !== -1;
				},
				sort : function(data1, data2, asc) {
					var value1 = data1.matched.text();
					var value2 = data2.matched.text();
					var sortDirection = (asc)? 1 : -1;
					return sortDirection * naturalSort(value1, value2);
				}
			}];
			columns.push(SampleDataGridUtil.createOperationsColumn());
			
			var getDataList = function(callback) {
				var dataList = [];
				var words = value.split(" ");
				var searchRegexpes = [];
				for(var sIdx = 0; sIdx < words.length; sIdx++) {
					var word = words[sIdx];
					searchRegexpes[sIdx] = new RegExp(word, "i");
				}
				
				var addMatchedPairs = function(matchedPairs, fieldName, fieldValue) {
					for(var tIdx = 0; tIdx < searchRegexpes.length; tIdx++) {
						if(searchRegexpes[tIdx].test(fieldValue)) {
							var match = {};
							match.name = fieldName;
							match.found = words[tIdx];
							match.value = fieldValue;
							matchedPairs.push(match);
						}
					}
				}
				
				for(var i = 0; i < data.length; i++) {
					var matchedPairs = [];
					var sample = data[i];
					
					addMatchedPairs(matchedPairs, "Code", sample.code); //Check Code
					addMatchedPairs(matchedPairs, "Sample Type", sample.sampleTypeCode); //Check Type
					
					//Check Properties
					for (propertyName in sample.properties) {
						var propertyValue = sample.properties[propertyName];
						addMatchedPairs(matchedPairs, "Property " + propertyName, propertyValue); //Check Properties
					}
					
					//Check date fields
					var regEx = /\d{4}-\d{2}-\d{2}/g;
					var match = value.match(regEx);
					if(match && match.length === 1) {
						var registrationDateValue = Util.getFormatedDate(new Date(sample.registrationDetails.registrationDate));
						if(registrationDateValue.indexOf(match[0]) !== -1) {
							matchedPairs.push({ name : "Registration Date", value : registrationDateValue, found : match[0]});
						}
						var modificationDateValue = Util.getFormatedDate(new Date(sample.registrationDetails.modificationDate));
						if(modificationDateValue.indexOf(match[0]) !== -1) {
							matchedPairs.push({ name : "Modification Date", value : modificationDateValue, found : match[0]});
						}
					}
					
					var $container = $("<p>");
					var score = 0;
					for(var mIdx = 0; mIdx < matchedPairs.length; mIdx++) {
						switch(matchedPairs[mIdx].name) {
							case "Code":
								score+= 1000;
								break;
							case "Sample Type":
								score+= 100;
								break;
							default:
								score+= 10;
								break;
							break;
						}
						if(mIdx < 0) {
							$container.append($("<br>"));
						}
						$container.append($("<p>").append($("<strong>").append(matchedPairs[mIdx].name + ": ")).append("Found \"" + matchedPairs[mIdx].found + "\" in \"" + matchedPairs[mIdx].value + "\""));
					}
					
					//properties
					dataList.push({
						id: sample.permId,
						permId : sample.permId,
						code : sample.code,
						score : score,
						sampleTypeCode : sample.sampleTypeCode,
						matched : $container
					});
				}
				
				dataList = dataList.sort(function(e1, e2) { 
					return e2.score - e1.score; 
				});
				callback(dataList);
			};
			
			var rowClick = function(e) {
				mainController.changeView('showViewSamplePageFromPermId', e.data.permId);
			}
			
			var dataGrid = new DataGridController("Search Results", columns, [], null, getDataList, rowClick, true, "SEARCH_OPENBIS", false, {
				fileFormat: DataGridExportOptions.FILE_FORMAT.TSV,
				filePrefix: 'search'
			}, 90);
			localReference.currentView = dataGrid;
			var content = localReference._getBackwardsCompatibleMainContainer();
			dataGrid.init(content);
			history.pushState({id: ++this._stateCounter}, "", ""); //History Push State
		});
	}
	
	this.getScrollbarWidth = function() {
		if (this.scrollbarWidth === undefined) {
			var inner = document.createElement('p');
			inner.style.width = "100%";
			inner.style.height = "200px";
			
			var outer = document.createElement('div');
			outer.style.position = "absolute";
			outer.style.top = "0px";
			outer.style.left = "0px";
			outer.style.visibility = "hidden";
			outer.style.width = "200px";
			outer.style.height = "150px";
			outer.style.overflow = "hidden";
			outer.appendChild (inner);
			
			document.body.appendChild (outer);
			var w1 = inner.offsetWidth;
			outer.style.overflow = 'scroll';
			var w2 = inner.offsetWidth;
			if (w1 == w2) w2 = outer.clientWidth;
			
			document.body.removeChild (outer);
			this.scrollbarWidth = w1 - w2;
		}
		return this.scrollbarWidth;
	}

}