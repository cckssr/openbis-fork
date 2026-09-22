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

        if(!$container.attr('id')) {
            $container.attr('id', 'mainHeader-' + mainController.getNextId())
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

        var openEntityCallback = function(entityId) {
            console.log("OPENING ENTITY:" + entityId);
            require([ "as/dto/sample/search/SampleSearchCriteria", "as/dto/sample/fetchoptions/SampleFetchOptions",
                    "as/dto/experiment/search/ExperimentSearchCriteria", "as/dto/experiment/fetchoptions/ExperimentFetchOptions",
                    "as/dto/dataset/search/DataSetSearchCriteria", "as/dto/dataset/fetchoptions/DataSetFetchOptions"
                ],
                function(SampleSearchCriteria, SampleFetchOptions,
                         ExperimentSearchCriteria, ExperimentFetchOptions,
                         DataSetSearchCriteria, DataSetFetchOptions) {

                    let sampleCriteria = new SampleSearchCriteria()
                    let experimentCriteria = new ExperimentSearchCriteria();
                    let dataSetCriteria = new DataSetSearchCriteria();

                    if(entityId.startsWith("/")) {
                        sampleCriteria.withIdentifier().thatEquals(entityId)
                        experimentCriteria.withIdentifier().thatEquals(entityId)
                        $.when(
                            mainController.openbisV3.searchSamples(sampleCriteria, new SampleFetchOptions()),
                            mainController.openbisV3.searchExperiments(experimentCriteria, new ExperimentFetchOptions()),
                        ).then(function(
                            sampleResult,
                            experimentResult) {
                            if(sampleResult.getTotalCount() > 0) {
                                mainController.changeView('showViewSamplePageFromPermId', sampleResult.getObjects()[0].permId.permId)
                            } else if(experimentResult.getTotalCount() > 0) {
                                mainController.changeView('showExperimentPageFromPermId', experimentResult.getObjects()[0].permId.permId);
                            } else {
                                //not found
                                console.log("ENTITY NOT FOUND:" + entityId);
                            }

                        })


                    } else {


                        sampleCriteria.withPermId().thatEquals(entityId)
                        experimentCriteria.withPermId().thatEquals(entityId)
                        dataSetCriteria.withPermId().thatEquals(entityId)

                        $.when(
                            mainController.openbisV3.searchSamples(sampleCriteria, new SampleFetchOptions()),
                            mainController.openbisV3.searchExperiments(experimentCriteria, new ExperimentFetchOptions()),
                            mainController.openbisV3.searchDataSets(dataSetCriteria, new DataSetFetchOptions())
                        ).then(function(
                            sampleResult,
                            experimentResult,
                            dataSetResult) {
                                if(sampleResult.getTotalCount() > 0) {
                                    mainController.changeView('showViewSamplePageFromPermId', sampleResult.getObjects()[0].permId.permId)
                                } else if(dataSetResult.getTotalCount() > 0 ) {
                                    mainController.changeView('showViewDataSetPageFromPermId', dataSetResult.getObjects()[0].permId.permId);
                                } else if(experimentResult.getTotalCount() > 0) {
                                    mainController.changeView('showExperimentPageFromPermId', experimentResult.getObjects()[0].permId.permId);
                                } else {
                                    //not found
                                    console.log("ENTITY NOT FOUND:" + entityId);
                                }

                        })


                    }


                // case "SAMPLE":
                //     mainController.changeView('showViewSamplePageFromPermId', permId);
                //     break;
                // case "EXPERIMENT":
                //     mainController.changeView('showExperimentPageFromPermId', permId);
                //     break;

                    // mainController.openbisV3.searchSamples(criteria, new SampleFetchOptions())
                    //     .done(function(sampleResults) {
                    //         if(sampleResults.getObjects().length > 0) {
                    //
                    //         } else {
                    //             // dataset
                    //         }
                    //
                    //
                    // })

                //         sampleResults = await openbis.searchSamples(criteria, fo)

                });
        }

        let tabs = [];
        if (!profile.isAdmin) {
            if(profile.mainMenu.showLabNotebook) {
                tabs.push({page: "lab_notebook", label: "Lab Notebook"})
            }
            if(profile.mainMenu.showInventory || profile.mainMenu.showStock) {
                tabs.push({page: "lims", label: "Inventory"})
            }
            if(profile.mainMenu.showTools) {
                tabs.push({page: "tools", label: "Tools"})
            }
        } else {
            tabs = [
                {page: "lab_notebook", label: "Lab Notebook"},
                {page: "lims", label: "Inventory"},
                {page: "tools", label: "Tools"},
            ];
        }

        let props = {
            pageChangeFunction: this._controller.handlePageChange,
            searchFunction: this._controller.searchFunction,
            logoutFunction: this._controller.handleLogout,
            searchText: "",
            currentPage: this._controller.getCurrentPage(),
            userName: mainController.serverFacade.getUserId(),
            tabs: tabs,
            barcodeFunction: barcodeFunction,
            showChatbot: profile.mainMenu.showChatAgent,
            sendMessageCallback: sendChatBotMessage,
            openEntityCallback: openEntityCallback,
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
        return NgComponentsManager.renderComponent(Menu, $container.get(0), true);
    }

    this._searchByIds = function(identifiers, callback) {
        //search Experiments and Samples by a would-be identifiers and returns links to them
        require([ "as/dto/sample/id/SampleIdentifier", "as/dto/sample/id/SamplePermId", "as/dto/sample/fetchoptions/SampleFetchOptions",
                "as/dto/experiment/id/ExperimentIdentifier", "as/dto/experiment/id/ExperimentPermId", "as/dto/experiment/fetchoptions/ExperimentFetchOptions"],
            function(SampleIdentifier, SamplePermId, SampleFetchOptions, ExperimentIdentifier, ExperimentPermId, ExperimentFetchOptions) {
                var sampleFetchOptions = new SampleFetchOptions();
                sampleFetchOptions.withProperties();
                var ids = identifiers.map(id => {
                    if(id.startsWith('/')) {
                        return new SampleIdentifier(id);
                    }
                    return new SamplePermId(id);
                });

                mainController.openbisV3.getSamples(ids, sampleFetchOptions).done(function(sampleResults) {
                    let links = {};
                    let missing = []
                    var samples = Util.mapValuesToList(sampleResults);
                    for ( let sample of samples) {
                        if(identifiers.includes(sample.permId.permId)) {
                            identifiers = identifiers.filter(x => x != sample.permId.permId);
                        } else if(identifiers.includes(sample.identifier.identifier)) {
                            identifiers = identifiers.filter(x => x != sample.identifier.identifier);
                        }
                        var link = FormUtil.getFormLink(sample.identifier.identifier, 'Sample', sample.permId.permId, null);
                        var name = sample.properties['NAME'] ?? null;
                        name = sample.identifier.identifier + (name ? ' (' + name + ')' : '');
                        links[sample.identifier.identifier] = {link: link, name: name, identifier: sample.identifier.identifier};
                        links[sample.permId.permId] = {link: link, name: name, identifier: sample.identifier.identifier};
                    }
                    if(identifiers.length == 0) {
                        callback(links);
                    } else {
                        var experimentFetchOptions = new ExperimentFetchOptions();
                        experimentFetchOptions.withProperties();
                        var ids = identifiers.map(id => {
                            if(id.startsWith('/')) {
                                return new ExperimentIdentifier(id);
                            }
                            return new ExperimentPermId(id);
                        });

                        mainController.openbisV3.getExperiments(ids, experimentFetchOptions).done(function(experimentResults) {
                            var experiments = Util.mapValuesToList(experimentResults);
                            for ( let experiment of experiments) {
                                var link = FormUtil.getFormLink(experiment.identifier.identifier, 'Experiment', experiment.identifier.identifier, null);
                                var name = experiment.properties['NAME'] ?? null;
                                name = experiment.identifier.identifier + (name ? ' (' + name + ')' : '');
                                links[experiment.identifier.identifier] = {link: link, name: name, identifier: experiment.identifier.identifier};
                                links[experiment.permId.permId] = {link: link, name: name, identifier: experiment.identifier.identifier };
                            }
                            callback(links);
                        }).fail(function(result) {
                            callback(links);
                        });
                    }
                }).fail(function(result) {
                    callback({});
                });
            });
    }

}