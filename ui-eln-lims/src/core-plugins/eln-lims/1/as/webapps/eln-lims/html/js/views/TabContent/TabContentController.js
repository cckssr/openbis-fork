function TabContentController(controller) {
    this._controller = controller;
    this._model = new TabContentModel();
    this._tabControllerMap = {
        "lab_notebook": new TabController('lab'),
        "lims": new TabController('lims'),
        "tools": new TabController('tools')
    }
    this._tabController = this._tabControllerMap['lab_notebook'];

    this._view = new TabContentViewer(this);

    var _this = this;

    this.init = function ($container, initCallback) {
        _this._model.$container = $container;
         _this._view.repaint($container)
        initCallback()

    }

    this.changePage = function(page) {
        this._tabController = this._tabControllerMap[page];
        this._view.changeTabView(page);
        let state = this._tabController.getState();
        let openTabs = state.openTabs;
        if(openTabs) {
            this._model.currentTab = openTabs.filter(state.selectedTab)[0]
            this._updateView(this._model.currentTab)
        }
    }

    this.openTab = function(tabInfo, callBack) {
        this._tabController.openTab(tabInfo, (currentTab) => {
            _this._model.currentTab = currentTab
            callBack();
        });
    }

    this.closeCurrentTab = function() {
        var tab = this._model.currentTab;
        this._tabController.component.handleTabClose(tab);
    }

    this.updateCurrentTabInfo = function(tabInfo) {
        var tab = _this._model.currentTab;
        tab.label = tabInfo.label;
        tab.changed = tabInfo.changed ? tabInfo.changed : false;
        tab.icon = tabInfo.icon ? tabInfo.icon : null;
        tab.node = mainController.sideMenu.getCurrentNodeId();
        tab.tree = mainController.sideMenu.getCurrentTree();
        tab.view = mainController.currentView;
        tab.finalize = tabInfo.finalize;
        this._tabController.openTab(tab, (currentTab) => {
            _this._model.currentTab = currentTab
        });
    }

    this.updateView = function(view) {
        if(view && view.tabId) {
            this._model.views[view.tabId] = view;
        }
    }

    this._updateView = function(tab) {
        var tabHeader = $("#" + tab.id + " #tab-content-header")
        var tabBody = $("#" + tab.id + " #tab-content-body")

        mainController.views.header = tabHeader;
        mainController.views.content = tabBody;
        mainController.currentView = _this._model.views[tab.id];

        if(tab.url) {
        //todo
            history.pushState({}, null, tab.url)
        }

        mainController.sideMenu.removeSubSideMenu();
        if(mainController.currentView && mainController.currentView.refresh) {
            mainController.currentView.refresh(mainController.views)
        }

    }

    this.handleTabSelect = function(tab) {
        _this._updateView(tab);

        if(mainController.sideMenu.getCurrentTree() !== tab.tree) {
//            mainController.mainHeader.navigateToTab(tab.tree.toUpperCase());
//            mainController.sideMenu.changeCurrentTree(tab.tree, JSON.parse(tab.node));
        } else {
            mainController.sideMenu.moveToNodeId(tab.node);
        }
        _this._model.currentTab = tab;
    }

    this.handleTabClose = function(tab, newSelectedTab) {
        if(newSelectedTab) {
            if(mainController.sideMenu.getCurrentTree() !== newSelectedTab.tree) {
                mainController.mainHeader.navigateToTab(newSelectedTab.tree.toUpperCase());
                mainController.sideMenu.changeCurrentTree(newSelectedTab.tree, JSON.parse(newSelectedTab.node));
            } else {
                mainController.sideMenu.moveToNodeId(newSelectedTab.node);
            }
        } else {
            mainController.sideMenu.moveToNodeId(null);
        }

        if(_this._model.views[tab.id]) {
            delete _this._model.views[tab.id]
        }

        if(tab.finalize) {
            tab.finalize();
        }

    }


}