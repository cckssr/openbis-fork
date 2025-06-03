function TabContentViewer(controller) {
    this._controller = controller;

    this.repaint = function ($container) {
        this._$container = $container

        this._$lab = $("<div>", { id: "tab-content-lab_notebook"})
        this._$inv = $("<div>", { id: "tab-content-inventory"})
        this._$tools = $("<div>", { id: "tab-content-tools"})

        $container.append(this._$lab).append(this._$inv).append(this._$tools)

        let props = {
            controller: this._controller._tabControllerMap["lab_notebook"],
            openTabs: [],
            selectedTab: (x) => false,
            page: '',
            style: {
                height: '50px',
                position: 'sticky',
            },
            handleTabSelect: this._controller.handleTabSelect,
            handleTabClose: this._controller.handleTabClose,
        }

        let SimpleContentLabNotebook = React.createElement(window.NgComponents.default.ElnContent, props)

        NgComponentsManager.renderComponent(SimpleContentLabNotebook, this._$lab.get(0));
        if(mainController.sideMenu.getCurrentTree() !== "lab_notebook") {
            this._$lab.hide()
        }

        props.controller = this._controller._tabControllerMap["lims"];
        let SimpleContentInventory = React.createElement(window.NgComponents.default.ElnContent, props)
        NgComponentsManager.renderComponent(SimpleContentInventory, this._$inv.get(0));
        if(mainController.sideMenu.getCurrentTree() !== "lims") {
            this._$inv.hide()
        }

        props.controller = this._controller._tabControllerMap["tools"];
        let SimpleContentTools = React.createElement(window.NgComponents.default.ElnContent, props)
        NgComponentsManager.renderComponent(SimpleContentTools, this._$tools.get(0));
        if(mainController.sideMenu.getCurrentTree() !== "tools") {
            this._$tools.hide()
        }

    }

    this.changeTabView = function (value) {
        if(value === "lab_notebook") {
            this._$lab.show();
            this._$inv.hide();
            this._$tools.hide();
        } else if(value === "lims") {
            this._$lab.hide();
            this._$inv.show();
            this._$tools.hide();
        } else if(value === "tools") {
            this._$lab.hide();
            this._$inv.hide();
            this._$tools.show();
        }

    }

}