function LoginView(controller) {
    this._mainController = controller

    var _this = this;

    this.init = function() {

        let serverInformation = {
            'authentication-service.switch-aai.link':  this._mainController.profile.singleSignOnUrlTemplate,
            'authentication-service.switch-aai.label': this._mainController.profile.singleSignOnLinkLabel
        }

        let props = {
            title: "Lab Notebook & Inventory\u00A0Manager",
            serverInformation: serverInformation,
            loginFunction: this.loginFunction,
            styles: {minWidth: '480px'}
        }

        let $container = $('#login-form');

        let Login = React.createElement(window.NgComponents.default.Login, props)
        this.Login = Login;
        NgComponentsManager.renderComponent(Login, $container.get(0));
    }

    this.loginFunction = function(username, password) {
        Util.blockUI();
        $("#mainHeader").show();
        $("#mainContainer").show();
        _this._mainController.serverFacade.login(
                username,
                password,
        function(data) { _this._mainController.enterApp(data, username, password) },
        function() { Util.unblockUI(); }
        );
    }

}