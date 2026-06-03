function RootNodeSettings(mode, profileToEdit)
{
    this._container = null;
    this._mode = mode;
    this._profileToEdit = profileToEdit;
    this.selected = null;
    this._advancedEntitySearchDropdown = null;

    var _refreshableFields = [];


    this.init = function($container) {
        var container = $("<div>").css("padding-left", "8px");
        $container.append(container);
        this._container = container;
        this.repaint(this._container);
    }

    this.repaint = function(container) {
        var _this = this;

        var radioDiv = $("<p>");
        var $inputDiv = $("<div>");

        function onRadioChange() {
            var value = $(this).val();
            _this.selected = { type: value };
            if(value === "identifier") {
                $inputDiv.empty();
                _this._advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(false, true, "search entity use as root node",
                    false, false, false, true, true);
                _this._advancedEntitySearchDropdown.onChange(function(selected) {
                    _this.selected = {type: value, value: selected[0]};
                });
                _this._advancedEntitySearchDropdown.init($inputDiv);
                if(_this._mode === FormMode.VIEW) {
                    $inputDiv.find("*").prop("disabled", true);
                }
            } else {
                _this._advancedEntitySearchDropdown = null;
                $inputDiv.empty();
            }

        }

        var $radioNone = $("<input>", { type: "radio", name: "rootNode", value: "none"}).on("change", onRadioChange);
        var $radioHomeSpace = $("<input>", { type: "radio", name: "rootNode", value: "homeSpace"}).on("change", onRadioChange);
        var $radioIdentifier = $("<input>", { type: "radio", name: "rootNode", value: "identifier"}).on("change", onRadioChange);

        if(this._mode === FormMode.VIEW) {
            $radioNone.prop("disabled", true);
            $radioHomeSpace.prop("disabled", true);
            $radioIdentifier.prop("disabled", true);
        }

        if(this._profileToEdit.rootNodeSettings) {
            if(this._profileToEdit.rootNodeSettings.type === "none") {
                $radioNone.attr("checked", "");
            } else if(this._profileToEdit.rootNodeSettings.type === "homeSpace") {
                $radioHomeSpace.attr("checked", "");
            } else {
                $radioIdentifier.attr("checked", "");
                $radioIdentifier.trigger("change");
                _this._advancedEntitySearchDropdown.addSelected(this._profileToEdit.rootNodeSettings.value);
            }
            this.selected = this._profileToEdit.rootNodeSettings;
        }

        $radioNone.refresh = function() {
            $(this).off("change");
            $(this).on("change", onRadioChange);
        }
        _refreshableFields.push($radioNone);

        $radioHomeSpace.refresh = function() {
            $(this).off("change");
            $(this).on("change", onRadioChange);
        }
        _refreshableFields.push($radioHomeSpace);

        $radioIdentifier.refresh = function() {
            $(this).off("change");
            $(this).on("change", onRadioChange);
        }
        _refreshableFields.push($radioIdentifier);



        radioDiv.append($("<label>", {style: "font-weight: normal;" }).append($radioNone).append($("<span>", {text: " None"}))).append($("<br>"));
        radioDiv.append($("<label>", {style: "font-weight: normal;" }).append($radioHomeSpace).append($("<span>", {text: " Home Space"}))).append($("<br>"));
        radioDiv.append($("<label>", {style: "font-weight: normal;" }).append($radioIdentifier).append($("<span>", {text: " Identifier"}))).append($("<br>"));

        radioDiv.append($inputDiv);
        container.append(FormUtil.getFieldForComponentWithLabel(radioDiv, ""));
    }

    this.refresh = function() {
        if(this._advancedEntitySearchDropdown) {
            this._advancedEntitySearchDropdown.refresh();
        }
        for(var field of _refreshableFields) {
            field.refresh();
        }

    }

    this.getValue = function() {
        return this.selected;
    }
}