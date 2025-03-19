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

/**
 * Creates an instance of SideMenuWidgetView.
 *
 * @constructor
 * @this {SideMenuWidgetView}
 */
function SideMenuWidgetView(sideMenuWidgetController, sideMenuWidgetModel) {
    this._sideMenuWidgetController = sideMenuWidgetController
    this._sideMenuWidgetModel = sideMenuWidgetModel
    this.__this = this;
    this.subSideMenuViewer = null;
    this._resizeObserver = new ResizeObserver((entries) => {
                   var width = entries[0].contentRect.width;
                   if(width < 55) {
                        LayoutManager.setColumnSize(55, $(window).width() - 55, null);
                        if(!mainController.sideMenu.isCollapsed) {
                            mainController.sideMenu.collapseSideMenu();
                        }
                   } else if(width > 55 && mainController.sideMenu.isCollapsed && !LayoutManager.isMobile()) {
                        mainController.sideMenu.expandSideMenu();
                   } else if(width == 55 && !mainController.sideMenu.isCollapsed) {
                        mainController.sideMenu.collapseSideMenu();
                   }
               });

    var MIN_LENGTH = 3

    this.repaint = function ($container, isCollapsed) {
        this._$container = $container
        this._resizeObserver.disconnect();

        if(isCollapsed && !LayoutManager.isMobile()) {
            mainController.sideMenu.isCollapsed = true;
            var width = $(window).width();
            LayoutManager.firstColumnResize = 55;
            LayoutManager.secondColumnResize = width - 55;
            this._collapsedSideMenu($container);
        } else {
            mainController.sideMenu.isCollapsed = false;
            //
            // Fix Header
            //
            var $widget = $("<div>", { id: "sideMenuTopContainer" })
            var _this = this;
            $widget.css("height", "100%")
            $widget.css("display", "flex")
            $widget.css("flex-direction", "column")

            if(LayoutManager.firstColumn && LayoutManager.firstColumn.width() <= 55) {
                var width = $(window).width();
                var firstColumnWidth = width * 0.25;
                LayoutManager.firstColumnResize = firstColumnWidth;
                LayoutManager.secondColumnResize = (width - firstColumnWidth - 1);
            }




            var $header = $("<div>", { id: "sideMenuHeader" })
            $header.css("background-color", "rgb(248, 248, 248)")
            $header.css("padding", "10px")
            var searchDomains = profile.getSearchDomains()

            var searchFunction = function () {
                var searchText = $("#search").val()
                if (searchText.length < MIN_LENGTH) {
                    Util.showInfo(
                        "The minimum length for a global text search is " + MIN_LENGTH + " characters.",
                        function () {},
                        true
                    )
                    return false
                }

                var domainIndex = $("#search").attr("domain-index")
                var searchDomain = null
                var searchDomainLabel = null

                if (domainIndex) {
                    searchDomain = profile.getSearchDomains()[domainIndex].name
                    searchDomainLabel = profile.getSearchDomains()[domainIndex].label
                } else {
                    searchDomain = profile.getSearchDomains()[0].name
                    searchDomainLabel = profile.getSearchDomains()[0].label
                }

                var argsMap = {
                    searchText: searchText,
                    searchDomain: searchDomain,
                    searchDomainLabel: searchDomainLabel,
                }
                var argsMapStr = JSON.stringify(argsMap)

                mainController.changeView("showSearchPage", argsMapStr)
            }

            var dropDownSearch = null
            if (searchDomains.length > 0) {
                //Prefix function
                var selectedFunction = function (selectedSearchDomain, domainIndex) {
                    return function () {
                        var $search = $("#search")
                        $search.attr("placeholder", selectedSearchDomain.label + " Search")
                        $search.attr("domain-index", domainIndex)
                    }
                }

                //Dropdown elements
                var dropDownComponents = []
                for (var i = 0; i < searchDomains.length; i++) {
                    dropDownComponents.push({
                        href: selectedFunction(searchDomains[i], i),
                        title: searchDomains[i].label,
                        id: searchDomains[i].name,
                    })
                }

                dropDownSearch = FormUtil.getDropDownToogleWithSelectedFeedback(
                    null,
                    dropDownComponents,
                    true,
                    searchFunction
                )
                dropDownSearch.change()
            }

            var searchElement = $("<input>", {
                id: "search",
                type: "text",
                class: "form-control search-query",
                placeholder: "Global Search",
            })
            searchElement.keypress(function (e) {
                var key = e.which
                var onFocus = searchElement.is(":focus")
                var searchString = searchElement.val()
                if (
                    key == 13 && // the enter key code
                    onFocus && // ensure is focused
                    searchString.length >= MIN_LENGTH
                ) {
                    // min search length of 3 characters
                    searchFunction()
                    return false
                } else if (key == 13 && onFocus && searchString.length < MIN_LENGTH) {
                    Util.showInfo(
                        "The minimum length for a global text search is " + MIN_LENGTH + " characters.",
                        function () {},
                        true
                    )
                    return false
                }
            })
            searchElement.css({ display: "inline" })
            searchElement.css({ "margin-left": "2px" })
            searchElement.css({ "margin-right": "2px" })

            var logoutButton = this._logoutButton();

            var barcodeReaderBtn = this._barcodeReaderBtn();

            var $searchForm = $("<form>", { onsubmit: "return false;" })
                .append(logoutButton)
                .append(searchElement)
                .append(dropDownSearch)

            if(profile.mainMenu.showBarcodes) {
                $searchForm.append("&nbsp;")
                $searchForm.append(barcodeReaderBtn)
            }

            if(LayoutManager.isMobile()) {
                $searchForm.append("&nbsp;")
                $searchForm.append(this._collapseMenuBtn())
            }

            $searchForm.css("width", "100%")
            $searchForm.css("display", "flex")

            $header.append($searchForm)

            var $body = $("<div>", { id: "sideMenuBody" })
            $body.css("overflow-y", "auto")
            $body.css("flex", "1 1 auto")

            LayoutManager.addResizeEventHandler(function () {
                if (LayoutManager.FOUND_SIZE === LayoutManager.MOBILE_SIZE) {
                    $body.css("-webkit-overflow-scrolling", "auto")
                } else {
                    $body.css("-webkit-overflow-scrolling", "touch")
                }
            })



            $widget.append($header).append($body)

            if(this._sideMenuWidgetModel.subSideMenu) {
                var subSideMenu = this._sideMenuWidgetModel.subSideMenu;
                subSideMenu.css("margin-left", "3px")
                this._sideMenuWidgetModel.percentageOfUsage = 0.5
                $widget.append(subSideMenu)
                this._sideMenuWidgetController.resizeElement($body, 0.5)
                this._sideMenuWidgetController.resizeElement(subSideMenu, 0.5)
                if(this.subSideMenuViewer && this.subSideMenuViewer.init) {
                    this.subSideMenuViewer.init();
                }
            }

            if(!LayoutManager.isMobile()) {
             $widget.append(this._expandedFooter())
            }

            $container.empty()
            $container.css("height", "100%")
            $container.append($widget)

            //
            // Print Menu
            //
            this._sideMenuWidgetModel.menuDOMBody = $body
            this.repaintTreeMenuDinamic()

            if(this._resizeObserver && !LayoutManager.isMobile()) {
                this._resizeObserver.observe($widget[0]);
            }
        }
    }

    this._expandedFooter = function() {
        // Footer
        var $footer = $("<div>", { id: "sideMenuFooter"})
        $footer.css("background-color", "rgb(248, 248, 248)");
        $footer.css("height", "40px");
        $footer.css("align-content", "center");

        var footerToolbar = [];

        var $btn = null;
        $btn = $("<a>", { 'class' : 'btn btn-showhide' });
        $btn.css("height", "40px");
        $btn.css("padding-right", "20px");
        $btn.css("align-content", "end");

        var tooltip = "Hide menu"
        var id = "show-hide-menu-id"
        var iconType = IconUtil.getToolbarIconType("HIDE");
        var icon = IconUtil.getIcon(iconType);
        $btn.append(icon);
        $btn.attr("title", "Collapse sidebar")
        $btn.attr("id", "show-hide-menu-id");

        var _this = this;
        $btn.click(function() {
            _this._resizeObserver.disconnect();
            LayoutManager.setColumnSize(55, $(window).width() - 55, null);
            mainController.sideMenu.isCollapsed = true;
            _this._sideMenuWidgetController._browserController._saveSettings();
            _this._collapsedSideMenu(_this._$container)
        });

        var $toolbarContainer = $("<span>").append($btn);
        $footer.append($toolbarContainer.css("float", "right"));
        return $footer;
    }

    this._collapsedSideMenu = function($container) {
        var $widget = $("<div>", { id: "sideMenuTopContainer" })
        $widget.css("height", "100%")
        $widget.css("display", "flex")
        $widget.css("flex-direction", "column")
        //
        // Fix Header
        //
        var $header = $("<div>", { id: "sideMenuHeader" })
        $header.css("background-color", "rgb(248, 248, 248)")
        $header.css("padding", "10px")

        $header.append(this._logoutButton().css("min-width", "38px").css("width", "38px"));

        if(profile.mainMenu.showBarcodes) {
            $header.append("&nbsp;")
            $header.append(this._barcodeReaderBtn().css("min-width", "38px").css("width", "38px"))
        }


        var $body = $("<div>", { id: "sideMenuBody" })
        $body.css("overflow-y", "auto")
        $body.css("flex", "1 1 auto")
        $body.css("background-color", "rgb(248, 248, 248)")


        var $footer = $("<div>", { id: "sideMenuFooter"})
        $footer.css("background-color", "rgb(248, 248, 248)");
        $footer.css("height", "40px");
        $footer.css("align-content", "center");


        var $btn = null;
        $btn = $("<a>", { 'class' : 'btn btn-showhide' });
        $btn.css("height", "40px");
        $btn.css("padding-right", "20px");
        $btn.css("align-content", "end");

        var tooltip = "Show menu"
        var iconType = IconUtil.getToolbarIconType("SHOW");
        var icon = IconUtil.getIcon(iconType);
        $btn.append(icon);
        $btn.attr("title", "Show sidebar")
        $btn.attr("id", "show-hide-menu-id");

        var _this = this;
        $btn.click(function() {
                _this._resizeObserver.disconnect();
                _this._sideMenuWidgetController._browserController._saveSettings();
                mainController.sideMenu.isCollapsed = false;
                LayoutManager.restoreStandardSize();
                _this.repaint(_this._$container, false);
        });

        var $toolbarContainer = $("<span>").append($btn);
        $footer.append($toolbarContainer.css("float", "right"));


        $widget.append($header).append($body).append($footer)

        $container.empty()
        $container.css("height", "100%")
        $container.append($widget)

        if(this._resizeObserver) {
            this._resizeObserver.observe($widget[0]);
        }

    }

    this._logoutButton = function() {
        var option = mainController.loggedInAnonymously ? "LOGIN" : "LOGOUT";
        var tooltip = mainController.loggedInAnonymously ? "Login" : "Logout";
        var logoutButton = FormUtil.getToolbarButton(
            option,
            function () {
                $("body").addClass("bodyLogin")
                sessionStorage.setItem("forceNormalLogin", mainController.loggedInAnonymously)
                mainController.loggedInAnonymously = false
                mainController.serverFacade.logout()
            },
            null,
            tooltip,
            "logoutBtn"
        )
        return logoutButton;
    }

    this._barcodeReaderBtn = function() {
        var barcodeReaderBtn = FormUtil.getToolbarButton(
            "BARCODE",
            function() {
                BarcodeUtil.readBarcodeFromScannerOrCamera();
            },
            null,
            "Scan Barcode/QR code",
            "barcodeReaderBtn"
        )
        return barcodeReaderBtn;
    }

    this._collapseMenuBtn = function() {
        var option = LayoutManager.fullScreenFlag ? "COLLAPSE_DOWN" : "COLLAPSE_UP";
        var collapseMenuBtn = FormUtil.getToolbarButton(
            option,
            function() {
                if(!LayoutManager.fullScreenFlag) {
                    LayoutManager.fullScreen();
                    this.children[0].textContent = 'keyboard_double_arrow_down'
                } else {
                    LayoutManager.restoreStandardSize();
                    this.children[0].textContent = 'keyboard_double_arrow_up'
                }
            },
            null,
            "Toggle menu",
            "collapseMenuBtn"
        )
        return collapseMenuBtn;
    }

    this.repaintCollapseButton = function($container) {
        var btn = $container.find("#collapseMenuBtn");
        if(LayoutManager.fullScreenFlag) {
            btn[0].children[0].textContent = 'keyboard_double_arrow_down'
        } else {
            btn[0].children[0].textContent = 'keyboard_double_arrow_up'
        }
    }

    this._renderDOMNode = function (params) {
        var { node, container } = params

        var $node = $("<div>").addClass("browser-node")

        if (node.icon) {
            var $icon = IconUtil.getIcon(node.icon, 22)
            $("<div/>").addClass("browser-node-icon").append($icon).appendTo($node)
        }

        var text = null

        if (node.text !== null && node.text !== undefined) {
            text = node.text
        }

        if (node.view && node.object) {
            var menuId = JSON.stringify(node.object)
            var href = Util.getURLFor(menuId, node.view, node.viewData)
            var $link = $("<a>", {
                href: href,
                class: "browser-compatible-javascript-link browser-compatible-javascript-link-tree",
            }).text(text)
            $("<div/>").append($link).addClass("browser-node-text").css("align-content", "center").appendTo($node)
        } else {
            $("<div/>").text(text).addClass("browser-node-text").css("align-content", "center").appendTo($node)
        }

        $(container).empty().append($node)
    }

    this.repaintTreeMenuDinamic = function () {
        var _this = this

        this._sideMenuWidgetModel.menuDOMBody.empty().css("border-top", "1px solid #dbdbdb")

        var BrowserElement = React.createElement(window.NgComponents.default.Browser, {
                controller: _this._sideMenuWidgetController._browserController,
                renderDOMNode: _this._renderDOMNode,
                styles: {
                    nodes: "sideMenuNodes",
                },
            })

        NgComponentsManager.renderComponent(BrowserElement, this._sideMenuWidgetModel.menuDOMBody.get(0));
    }
}

var Images = {};
Images.decodeArrayBuffer = function(buffer, onLoad) {
    var mime;
    var a = new Uint8Array(buffer);
    var nb = a.length;
    if (nb < 4)
        return null;
    var b0 = a[0];
    var b1 = a[1];
    var b2 = a[2];
    var b3 = a[3];
    if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47)
        mime = 'image/png';
    else if (b0 == 0xff && b1 == 0xd8)
        mime = 'image/jpeg';
    else if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46)
        mime = 'image/gif';
    else
        return null;
    var binary = "";
    for (var i = 0; i < nb; i++)
        binary += String.fromCharCode(a[i]);
    var base64 = window.btoa(binary);
    var image = new Image();
    image.onload = onLoad;
    image.src = 'data:' + mime + ';base64,' + base64;
    image.Uint8Array = a;
    return image;
}