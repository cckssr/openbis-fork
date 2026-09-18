
//
// NgComponents manager that keeps track of rendered React components and cleans them up periodically when they are no longer used
//

window.NgComponentsManager = new function(){
    var thisManager = this

    this.components = []

    this.renderComponent = function(reactComponent, domContainer, isNotRemovable){
        var reactComponentWithProviders = React.createElement(
            window.NgComponents.default.StyledEngineProvider,
            { injectFirst : true },
            React.createElement(
                window.NgComponents.default.ThemeProvider,
                {},
                React.createElement(
                    window.NgComponents.default.DatePickerProvider,
                    {},
                    reactComponent)
            )
        )

        var foundComponent = this.components.find(function(component){
            return component.domContainer === domContainer
        });

        if(foundComponent && foundComponent.isNotRemovable && !foundComponent.dedicatedDomContainer.isConnected) {
            foundComponent.isNotRemovable = false;
            foundComponent = null;
        }

        if(foundComponent && foundComponent.isNotRemovable){
            foundComponent.reactRoot.render(reactComponentWithProviders)
            return foundComponent.reactRoot;
        }else{
            var dedicatedDomContainer = document.createElement("div")
            dedicatedDomContainer.style.width = "100%"
            dedicatedDomContainer.style.height = "100%"
            domContainer.appendChild(dedicatedDomContainer)

            var reactRoot = ReactDOM.createRoot(dedicatedDomContainer)
            reactRoot.render(reactComponentWithProviders)
            this.components.push({ reactRoot: reactRoot, domContainer: domContainer, dedicatedDomContainer: dedicatedDomContainer, url: window.location.href, isNotRemovable: isNotRemovable })

            return reactRoot;
        }
    }

    this.cleanupComponents = function(){
        var stillConnectedComponents = []
        var backStack = [];
        if(mainController) {
            backStack = mainController.backStack;
        }
        this.components.forEach(function(component){
            if(component.isNotRemovable){
                stillConnectedComponents.push(component)
            } else if (component.dedicatedDomContainer.isConnected) {
                stillConnectedComponents.push(component)
            } else if(backStack.some(x => decodeURI(x.url) === decodeURI(component.url))) {
                stillConnectedComponents.push(component)
            } else {
                component.reactRoot.unmount()
            }
        })

        this.components = stillConnectedComponents
    }

    setInterval(function(){
        thisManager.cleanupComponents()
    }, 1000)
}

//
// Application Startup
//

window.addEventListener('popstate', function(event) {
    if (sessionStorage.getItem("normalLoginHasBeenForces") == "true") {
        location.reload();
    }
});
var profile = null;
var mainController = null;

var startELNLIMS = function() {
    mainController = new MainController(profile);
    mainController.initAppBeforeLogin(function(){
        // Global links handler - This function avoid normal link behaviour for javascript enabled links that will use ajax calls for left clicks, allowing to open them on a new tab with right click.
        $(document).click(function(e) {
            var elementClasses = $(e.target).attr('class');
            var isLeftClick = e.which === 1;
            if(isLeftClick && elementClasses && elementClasses.indexOf("browser-compatible-javascript-link") !== -1) {
                e.preventDefault();
                e.stopPropagation();
            }
        });

        $(document).ajaxError(function( event, jqxhr, settings, thrownError ) {
            if(settings.settings.suppressErrors) {
                return;
            }
            try {
                Util.showError("AJAX Error status: " + jqxhr.status + " - Status text: " + jqxhr.statusText + " - Calling: " + settings.url);
            } catch(err) {
                Util.showError("Unknown AJAX Error");
            }
        });

        new LoginView(mainController).init();

        $('#main').hide();

        var queryString = Util.queryString();
        var test = queryString.test;
        var testWithLogin = queryString.testWithLogin;

        if (test == "true" || testWithLogin == "true" || $.cookie("suitename")) {
            sessionStorage.setItem("forceNormalLogin", "true");
        }

        mainController.serverFacade.ifRestoredSessionActive(function(data) {
            Util.blockUI();
            mainController.enterApp(data)
        });

        if (sessionStorage.getItem("forceNormalLogin") != "true" && sessionStorage.getItem("loggedInAnonymously") != "false") {
            mainController.serverFacade.getOpenbisV3(function(openbisV3) {
                openbisV3.loginAsAnonymousUser().done(function(sessionToken) {
                    mainController.serverFacade.openbisServer.useSession(sessionToken);
                    mainController.serverFacade.openbisServer.rememberSession();
                    mainController.loggedInAnonymously = true;
                    sessionStorage.setItem("loggedInAnonymously", "true");
                    mainController.enterApp({"result":true});
                });
            });
        } else {
            sessionStorage.setItem("normalLoginHasBeenForces", "true");
            sessionStorage.setItem("loggedInAnonymously", "false");
        }
        sessionStorage.removeItem("forceNormalLogin");


        //Automatic login if special parameters are given
        var user = queryString.user;
        var pass = queryString.pass;
        if(user && pass) {
            Util.blockUI();
            mainController.serverFacade.login(user, pass, function(data) { mainController.enterApp(data) });
        }
        //Reset password
        if (mainController.resetPasswordRequested()) {
            mainController.resetPassword();
        }

        var jenkins = queryString.jenkins;
        if (jenkins == "true") {
            $.cookie("report-to-jenkins", "true");
        }

        if (testWithLogin == "true") {
            TestProtocol.startAdminTests(true);
        } else if (test == "true") {
            TestProtocol.startAdminTests(false);
        }

        if ($.cookie("suitename") == "testId") {
            TestProtocol.startUserTests();
        }

        if ($.cookie("suitename") == "finishTest") {
            TestProtocol.finishTests();
        }
    })
}

$(document).ready(function() {
    var numPlugins = !PLUGINS_CONFIGURATION?1:1+PLUGINS_CONFIGURATION.extraPlugins.length;
    var pluginsOnLoad = false;

    var wait = null;
    wait = function() {
        if (!profile) {
            setTimeout(wait,100);
        } else {
            if(profile.plugins.length === numPlugins) {
                startELNLIMS();
            } else if(!pluginsOnLoad) {
                if(PLUGINS_CONFIGURATION && PLUGINS_CONFIGURATION.extraPlugins) {
                    for(var pIdx = 0; pIdx < PLUGINS_CONFIGURATION.extraPlugins.length; pIdx++) {
                        loadJSResorce("./plugins/" + PLUGINS_CONFIGURATION.extraPlugins[pIdx] + "/plugin.js");
                    }
                }
                pluginsOnLoad = true;
                setTimeout(wait,100);
            } else {
                setTimeout(wait,100);
            }
        }
    }
    wait();
});