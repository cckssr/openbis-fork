function MainHeaderController() {
    this._mainController = mainController
    this._mainHeaderModel = new MainHeaderModel()
    this._mainHeaderController = new MainHeaderMenu()
    this._mainHeaderView = new MainHeaderView(this)
    var _this = this;

    var MIN_LENGTH = 3

    //
    // Init method that builds the menu object hierarchy
    //
    this.init = function ($container, initCallback) {
        _this._mainHeaderModel.$container = $container;
        _this._mainHeaderModel.currentPage = "lab_notebook";

        _this._mainHeaderView.repaint($container)

        initCallback()

    }

    this.getCurrentPage = function() {
        return this._mainHeaderModel.currentPage;
    }

    this.handlePageChange = function(event, value) {
        _this._mainHeaderModel.currentPage = value;
        mainController.sideMenu.removeSubSideMenu();
        mainController.tabContent.changePage(value);
        var tabInfo = mainController.tabContent.getCurrentTabInfo();
        var node = JSON.parse(tabInfo.node);
        mainController.sideMenu.changeCurrentTree(value, node);
        if(mainController.sideMenu.isCollapsed) {
            mainController.sideMenu.expandSideMenu();
        }
    }

    this.setSearchDomains = function(searchDomains) {
        _this._mainHeaderModel.searchDomains = searchDomains;
        _this._mainHeaderModel.searchDomain = searchDomains[0];
    }

    this.handleSearchDomainChange = function(event) {
        var index = event.target.value;
        _this._mainHeaderModel.searchDomain = _this._mainHeaderModel.searchDomains[index];
    }

    this.searchFunction = function(page, searchText) {
        var page = page;
        var searchText = searchText;

        if (searchText.length < MIN_LENGTH) {
            Util.showInfo(
                "The minimum length for a global text search is " + MIN_LENGTH + " characters.",
                function () {},
                true
            )
            return false
        }

        var searchDomain = _this._mainHeaderModel.searchDomain.name;
        var searchDomainLabel = _this._mainHeaderModel.searchDomain.label;


        var argsMap = {
            searchText: searchText,
            searchDomain: searchDomain,
            searchDomainLabel: searchDomainLabel,
        }
        var argsMapStr = JSON.stringify(argsMap)

        mainController.changeView("showSearchPage", argsMapStr)

    }

    this.handleLogout = function() {
        mainController.sideMenu.finalize()
        $("body").addClass("bodyLogin")
        sessionStorage.setItem("forceNormalLogin", mainController.loggedInAnonymously)
        mainController.loggedInAnonymously = false
        mainController.serverFacade.logout()
    }

    this.navigateToTabByEntity = function(type, spaceCode, permId) {
        var tree = this._getTreeFromSpaceCode(spaceCode);
        var node = null
        if(permId) {
            node = {
                id: permId,
            };
            switch(type) {
                case "SPACE":
                    node.type = "SPACE"
                    break;
                case "PROJECT":
                    node.type = "PROJECT"
                    break;
                case "COLLECTION":
                    node.type = "EXPERIMENT"
                    break;
                case "OBJECT":
                    node.type = "SAMPLE"
                    break;
                case "DATASET":
                    node.type = "DATASET"
                    break;
            }
        }
        if(tree !== _this._mainHeaderModel.currentPage) {
            _this._mainHeaderModel.currentPage = tree;
            _this._mainHeaderController.changeTab(tree);
            _this._mainController.tabContent.changePage(tree)
            _this._mainController.sideMenu.changeCurrentTree(tree, node)
        }
    }

    this._getTreeFromSpaceCode = function(spaceCode) {
        if(spaceCode) {
            if(SettingsManagerUtils.isLabNotebookSpace(spaceCode)) {
                return "lab_notebook";
            } else {
                return "lims";
            }
        } else {
            return "lab_notebook";
        }
    }

    this.navigateToTab = function(type) {
        var tabTypes = ["LAB_NOTEBOOK", "LIMS", "TOOLS"]
        var tree = _this._mainHeaderModel.currentPage;
        if(tabTypes.includes(type)) {
            tree = type.toLowerCase();
        }
        if(tree !== _this._mainHeaderModel.currentPage) {
            _this._mainHeaderModel.currentPage = tree;
            _this._mainController.sideMenu.changeCurrentTree(tree)
            _this._mainController.tabContent.changePage(tree)
            _this._mainHeaderController.changeTab(tree);

        }
    }


}