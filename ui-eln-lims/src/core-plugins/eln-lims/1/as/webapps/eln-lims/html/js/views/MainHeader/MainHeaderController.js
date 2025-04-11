function MainHeaderController(sideMenu) {
    this.sideMenu = sideMenu;
    this._mainController = mainController
    this._mainHeaderModel = new MainHeaderModel()
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
        mainController.sideMenu._browserController.CURRENT_TREE = value;
        mainController.sideMenu._browserController.load()
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

//        var searchFunction = function () {
//        var searchText = $("#search").val()
        if (searchText.length < MIN_LENGTH) {
            Util.showInfo(
                "The minimum length for a global text search is " + MIN_LENGTH + " characters.",
                function () {},
                true
            )
            return false
        }

//        var domainIndex = $("#search").attr("domain-index")
        var searchDomain = _this._mainHeaderModel.searchDomain.name;
        var searchDomainLabel = _this._mainHeaderModel.searchDomain.label;

//        if (domainIndex) {
//            searchDomain = profile.getSearchDomains()[domainIndex].name
//            searchDomainLabel = profile.getSearchDomains()[domainIndex].label
//        } else {
//            searchDomain = profile.getSearchDomains()[0].name
//            searchDomainLabel = profile.getSearchDomains()[0].label
//        }

        var argsMap = {
            searchText: searchText,
            searchDomain: searchDomain,
            searchDomainLabel: searchDomainLabel,
        }
        var argsMapStr = JSON.stringify(argsMap)

        mainController.changeView("showSearchPage", argsMapStr)
//    }

    }

    this.handleLogout = function() {
        $("body").addClass("bodyLogin")
        sessionStorage.setItem("forceNormalLogin", mainController.loggedInAnonymously)
        mainController.loggedInAnonymously = false
        mainController.serverFacade.logout()
    }

}