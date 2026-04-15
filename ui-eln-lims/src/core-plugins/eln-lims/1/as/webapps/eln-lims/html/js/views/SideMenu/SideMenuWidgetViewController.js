
function SideMenuWidgetViewController(sideMenuWidgetController, sideMenuWidgetModel) {
    this._sideMenuWidgetController = sideMenuWidgetController
    this._sideMenuWidgetModel = sideMenuWidgetModel
    this.subSideMenuViewer = null;

    var MOBILE_MODE = $(window).width() < LayoutManager.TABLET_SIZE;

    var _this = this;

    this._sideMenuMap = {
            "lab_notebook": new SideMenuWidgetView("lab_notebook", this._sideMenuWidgetController, this._sideMenuWidgetController._browserControllerMap["lab_notebook"], this._sideMenuWidgetModel),
            "lims": new SideMenuWidgetView("lims", this._sideMenuWidgetController, this._sideMenuWidgetController._browserControllerMap["lims"], this._sideMenuWidgetModel),
            "tools": new SideMenuWidgetView("tools", this._sideMenuWidgetController, this._sideMenuWidgetController._browserControllerMap["tools"], this._sideMenuWidgetModel)
    };
    this.currentView = null;

    this._$lab = $("<div>", { id: "side-menu-lab_notebook"})
    this._$inv = $("<div>", { id: "side-menu-inventory"})
    this._$tools = $("<div>", { id: "side-menu-tools"})

    this.finalize = function() {
        for(let key of Object.keys(this._sideMenuMap)) {
            this._sideMenuMap[key].finalize();
        }
    }

    this.repaint = function ($container, isCollapsed) {

        if(MOBILE_MODE) {
            if(!this._$container) {
                $container.append(this._$lab);
                this._$container = $container;
            }
            this.currentView = this._sideMenuMap["lab_notebook"];
            this.currentView.repaint(this._$lab, isCollapsed);
            return;
        }

        var value = mainController.sideMenu.getCurrentTree();
        if(!this._$container) {

            $container.append(this._$lab).append(this._$inv).append(this._$tools)
            this._sideMenuMap["lab_notebook"].repaint(this._$lab, isCollapsed);
            this._sideMenuMap["lims"].repaint(this._$inv, isCollapsed);
            this._sideMenuMap["tools"].repaint(this._$tools, isCollapsed);

            this._$container = $container
            $container.css("height", "100%");
            this.currentView = this._sideMenuMap[value];
        } else {
            this._$container = $container
            $container.css("height", "100%");


            var view = this._sideMenuMap[value];
            var container = null;
            if(value === "lab_notebook") {
                container = this._$lab;
            } else if(value === "lims") {
                container = this._$inv;
            } else if(value === "tools") {
                container = this._$tools;
            }
            if(view) {
                view.repaint(container, isCollapsed)

            }
            this.currentView = view;

        }


    }

    this._expandedFooter = function() {
        return this.currentView._expandedFooter();
    }

    this.changeSideMenuView = function (value) {
        if(MOBILE_MODE) {
            return;
        }
        this._sideMenuMap["lab_notebook"].disconnectObserver();
        this._sideMenuMap["lims"].disconnectObserver();
        this._sideMenuMap["tools"].disconnectObserver();
        if(value === "lab_notebook") {
            this._$lab.show();
            this._$inv.hide();
            this._$tools.hide();
            this._sideMenuMap["lab_notebook"].connectObserver();
            this.currentView = this._sideMenuMap["lab_notebook"];
        } else if(value === "lims") {
            this._$lab.hide();
            this._$inv.show();
            this._$tools.hide();
            this._sideMenuMap["lims"].connectObserver();
            this.currentView = this._sideMenuMap["lims"];
        } else if(value === "tools") {
            this._$lab.hide();
            this._$inv.hide();
            this._$tools.show();
            this._sideMenuMap["tools"].connectObserver();
            this.currentView = this._sideMenuMap["tools"];
        }
        this._sideMenuWidgetController.expandSideMenu()
    }



}