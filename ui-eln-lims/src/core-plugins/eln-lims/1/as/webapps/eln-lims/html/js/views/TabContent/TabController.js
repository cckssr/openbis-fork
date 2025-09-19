class TabController extends window.NgComponents.default.ContentController {

    constructor(id) {
        super()
        this.id = id;
    }

    renderComponent(tab) {
        let arg = 'div'
        let ele = React.createElement(arg, { id: tab,
            style: {
                "overflow-x": "auto",
                "overflow-y": "auto",
                "width" : "100%",
                "height": LayoutManager.secondColumn.height() - 50
            }
        })
        return ele;

    }

    changePage(page) {
        this.setState({
            page: page,
        })
    }

    replaceTabs(oldId, newId, callBack) {
        var counter = 200;
        var _this = this;
        var repeatUntilSet = function() {
            if(counter <= 0) {
                return;
            }
            let state = _this.getState()
            if(state.loaded) {
                let openTabs = state.openTabs;
                if(!openTabs) {
                    return;
                }
                let oldIndex = -1;
                for(let i = 0; i< openTabs.length; i++)
                {
                    let tab = openTabs[i];
                    if(tab.id === oldId){
                        oldIndex = i;
                        break;
                    }
                }

                let newIndex = -1;
                var newTab = null;
                for(let i = 0; i< openTabs.length; i++)
                {
                    let tab = openTabs[i];
                    if(tab.id === newId){
                        newIndex = i;
                        newTab = tab;
                        break;
                    }
                }

                if(newIndex === -1) {
                    //not yet created, so create placeholder
                    newTab = {
                     id: newId,
                     label: '',
                    }
                    if(oldIndex > -1) {
                        openTabs[oldIndex] = newTab;
                    } else {
                        openTabs.push(newTab);
                    }
                } else {
                    if(oldIndex > -1) {
                        openTabs[oldIndex] = newTab;
                        openTabs.splice(newIndex, 1);
                    }
                }
                _this.setState({
                    openTabs: openTabs,
                    selectedTab: x => x.id === newId,
                    page: mainController.sideMenu.getCurrentTree(),
                    loaded: true,
                }).then(() => callBack(newTab))
            } else {
              counter--;
              setTimeout(repeatUntilSet, 50);
            }
        }
        repeatUntilSet();
    }

    openTab(tabInfo, callBack) {
        var counter = 200;
        var _this = this;
        var repeatUntilSet = function() {
            if(counter <= 0) {
                return;
            }
            let state = _this.getState()
           if(state.loaded) {
            let openTabs = state.openTabs;
            if(!openTabs) {
                openTabs = [];
            }
            let newTab = null;
            let newTabFlag = true;
            for(let tab of openTabs) {
                if(tab.id === tabInfo.id) {
                    tab.label = tabInfo.label;
                    tab.changed = tabInfo.changed ? tabInfo.changed : false;
                    tab.icon = tabInfo.icon ? tabInfo.icon : null;
                    tab.node = mainController.sideMenu.getCurrentNodeId();
                    tab.tree = mainController.sideMenu.getCurrentTree();
                    tab.view = mainController.currentView;
                    tab.finalize = tabInfo.finalize;
                    tab.wasSideMenuCollapsed = mainController.sideMenu.isCollapsed;
                    newTabFlag = false;
                    newTab = tab;
                    break;
                }
            }
            if(newTabFlag) {
                newTab = {
                     label: tabInfo.label,
                     changed: tabInfo.changed ? tabInfo.changed : false,
                     icon: tabInfo.icon ? tabInfo.icon : null,
                     id: tabInfo.id,
                     node: mainController.sideMenu.getCurrentNodeId(),
                     tree: mainController.sideMenu.getCurrentTree(),
                     finalize: tabInfo.finalize,
                     wasSideMenuCollapsed: mainController.sideMenu.isCollapsed,
                     url: window.location.search,
                 }
                openTabs.push(newTab);
            }
            _this.setState({
                openTabs: openTabs,
                selectedTab: x => x.id === tabInfo.id,
                page: mainController.sideMenu.getCurrentTree(),
                loaded: true,
            }).then(() => callBack(newTab))
           } else {
                counter--;
               setTimeout(repeatUntilSet, 50);
           }
        }
        repeatUntilSet();
    }

    getTabs() {
        return this.getState().openTabs;
    }



}