function MainHeaderView(controller) {
    this._controller = controller;

    this.repaint = function ($container) {
        this._$container = $container

        if(LayoutManager.isMobile()) {
            LayoutManager.hideMainHeader();
            return;
        } else {
            LayoutManager.showMainHeader();
        }

        $container.css('background-color', 'rgb(248, 248, 248)');
        $container.css('display', 'flex');
        $container.css('width', 'flex');

        var barcodeFunction = null;
        if(profile.mainMenu.showBarcodes) {
            barcodeFunction = () => { BarcodeUtil.readBarcodeFromScannerOrCamera(); }
        }

        var searchDomains = profile.getSearchDomains();

        this._controller.setSearchDomains(searchDomains);

        // Need to convert the callback-based API to a Promise-based one
        var sendChatBotMessage = function(message, sessionId) {
            return new Promise((resolve, reject) => {
                try {
                    mainController.serverFacade.sendChatBotMessage(message, sessionId, function(response) {
                        if (response && response.result) {
                            resolve(response.result);
                        } else {
                            reject(new Error('Invalid response from server'));
                        }
                    });
                } catch (error) {
                    reject(error);
                }
            });
        }

        let props = {
            pageChangeFunction: this._controller.handlePageChange,
            searchFunction: this._controller.searchFunction,
            logoutFunction: this._controller.handleLogout,

            searchText: "",
            currentPage: this._controller.getCurrentPage(),

            userName: mainController.serverFacade.getUserId(),

            controller: this._controller._mainHeaderController,

            tabs: [
                {page: "lab_notebook", label: "Lab Notebook"},
                {page: "lims", label: "Inventory"},
                {page: "tools", label: "Tools"},
            ],
            barcodeFunction: barcodeFunction,
            showChatbot: profile.mainMenu.showChatAgent,
            sendMessageCallback: sendChatBotMessage,
            searchDomains: searchDomains,
            searchDomainChangeFunction: this._controller.handleSearchDomainChange,
            menuStyles: {
                searchBox: {
                    width: '500px',
                    transition: "width 0.3s",
                },
                searchField: {
                    'fontSize': "14px",
                    height: '29.125px'
                }
            }
        }

        let Menu = React.createElement(window.NgComponents.default.Menu, props)
        return NgComponentsManager.renderComponent(Menu, $container.get(0));
    }

}