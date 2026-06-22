function ToolbarSettings(mode,  profileToEdit, toolbarSettings) {
    this._container = null;
    this._mode = mode;
    this._profileToEdit = profileToEdit;
    this.selected = null;
    this._toolbarSettings = toolbarSettings;
    this._pillButtons = [];
    this._toggleValue = false;
    this._$toggleAllBtn = null;

    var _refreshableFields = [];

    const groupedToolbars = {
        'Buttons':
            [
                {name: "CREATE_PROJECT", icon: "PROJECT"},
                {name: "CREATE_FOLDER", icon: "FOLDER"},
                {name: "CREATE_ENTRY", icon: "ENTRY"},
                {name: "CREATE_OTHER", icon: "ENTRY"},
                {name: "UPLOAD_DATASET", icon: "DATA"},
                {name: "EDIT", icon: "EDIT"},
            ],
        'Dropdown': [
            {name: "MOVE", icon: ""},
            {name: "DELETE", icon: ""},
            {name: "COPY", icon: ""},
            {name: "PRINT", icon: ""},
            {name: "BARCODE", icon: ""},
            {name: "HIERARCHY_GRAPH", icon: ""},
            {name: "HIERARCHY_TABLE", icon: ""},
            {name: "UPLOAD_DATASET_HELPER", icon: ""},
            {name: "TEMPLATES", icon: ""},
            {name: "EXPORT_ALL", icon: ""},
            {name: "EXPORT_METADATA", icon: ""},
            {name: "MANAGE_ACCESS", icon: ""},
            {name: "FREEZE", icon: ""},
            {name: "HISTORY", icon: ""},
        ],

    };

    this.refresh = function() {
        for(let field of _refreshableFields) {
            field.refresh();
        }
    }

    this.init = function($container) {
        var container = $("<div>");
        $container.append(container);
        this._container = container;

        this.repaint(this._container);
    }

    this.isAnySettingEnabled = function() {
        for (let setting of Object.keys(this._toolbarSettings)) {
            if(this._toolbarSettings[setting]) {
                return true;
            }
        }
        return false;
    }

    this.isAnySettingDisabled = function() {
        for (let setting of Object.keys(this._toolbarSettings)) {
            if(!this._toolbarSettings[setting]) {
                return true;
            }
        }
        return false;
    }

    this.repaint = function(container) {
        var _this = this;
        var toggleAll = function() {
            let isAnyDisabled = _this.isAnySettingDisabled();
            if(!_this._toggleValue ||  !isAnyDisabled) {
                _this._toggleValue = !_this._toggleValue;
            }
            _this._$toggleAllBtn.toggleClass('tbe-pill-on', _this._toggleValue).attr('aria-checked', _this._toggleValue ? 'true' : 'false');
            for(let pillButton of _this._pillButtons) {
                pillButton.toggleClass('tbe-pill-on', _this._toggleValue).attr('aria-checked', _this._toggleValue ? 'true' : 'false');
            }
            for(let setting of Object.keys(_this._toolbarSettings)){
                _this._toolbarSettings[setting] = _this._toggleValue;
            }
        }

        this._toggleValue = this.isAnySettingEnabled();

        this._$toggleAllBtn = $('<div>')
            .addClass('tbe-pill')
            .toggleClass('tbe-pill-on', this._toggleValue)
            .attr('role', 'checkbox')
            .attr('aria-checked', this._toggleValue)
            .text('Toggle all')
            .on('click', toggleAll);
        this._$toggleAllBtn.refresh = function() {
            $(this).off('click');
            $(this).on('click', toggleAll);
        }


        var $header = $('<div>').addClass('tbe-box-header').append(
            $('<span>').addClass('tbe-box-title').text('Toolbar') ,
        );

        if(_this._mode !== FormMode.VIEW) {
            $header.append(this._$toggleAllBtn);
            _refreshableFields.push(this._$toggleAllBtn);
        }

        var $toolbar = $("<div>", {"style": "padding-left:35px;"});

        var createPill = function(element) {
            let name = element.name;
            let value = _this._toolbarSettings[name];
            let key = LabelUtil.getToolbarLabelInfo(name);



            var $pill = $('<div>')
                .addClass('tbe-pill')
                .toggleClass('tbe-pill-on', value)
                .attr('data-key', name)
                .attr('role', 'checkbox')
                .attr('aria-checked', value ? 'true' : 'false')
                .attr('title', key.tooltip)

            if(IconUtil.hasToolbarIconType(element.icon)) {
                let iconType = IconUtil.getToolbarIconType(element.icon);
                let icon = IconUtil.getIcon(iconType);
                $pill.append(icon);
            }

            $pill.append($('<span>').addClass('tbe-pill-label').append(key.label))

            if(_this._mode === FormMode.VIEW) {
                $pill.css('cursor', 'auto');
            }


            var toggle = function () {
                if(_this._mode !== FormMode.VIEW) {
                    let value = !_this._toolbarSettings[$(this).attr('data-key')];
                    _this._toolbarSettings[$(this).attr('data-key')] = value;
                    $pill.toggleClass('tbe-pill-on', value).attr('aria-checked', value ? 'true' : 'false');

                    if (_this._toggleValue !== _this.isAnySettingEnabled()) {
                        _this._toggleValue = !_this._toggleValue;
                        _this._$toggleAllBtn.toggleClass('tbe-pill-on', _this._toggleValue).attr('aria-checked', _this._toggleValue ? 'true' : 'false');
                    }
                }
            };
            $pill.on('click', toggle);
            $pill.refresh = function() {
                $(this).off('click');
                $(this).on('click', toggle);
            }
            return $pill;
        }

        var settingKeys = new Set(Object.keys(_this._toolbarSettings));
        var keys = new Set();

        Object.keys(groupedToolbars).forEach(toolbarGroupName => {
            var hasTag = false;
            var group = $("<span>", {"style": "font-size:medium; font-weight: bold;"})
                .append($("<span>", {"style": "margin-left:17px"}).text(toolbarGroupName+": "));
            groupedToolbars[toolbarGroupName].forEach(tag => {
                if(settingKeys.has(tag.name)) {
                    hasTag = true;
                    keys.add(tag.name);
                    let pill = createPill(tag);
                    this._pillButtons.push(pill);
                    group.append(pill);
                    _refreshableFields.push(pill)
                }
            })

            if(toolbarGroupName === 'Dropdown') {
                for(let setting of Object.keys(_this._toolbarSettings).sort()) {
                    if(!keys.has(setting)) {
                        hasTag = true;
                        keys.add(setting);
                        let pill = createPill({name:setting, icon:null});
                        this._pillButtons.push(pill);
                        group.append(pill);
                        _refreshableFields.push(pill)
                    }
                }
            }

            if(hasTag) {
                $toolbar.append(group);
            }
        });

        container.append($header);
        container.append($toolbar);
        container.append($("<br>"));
    }

    this.getValue = function() {
        return this._toolbarSettings;
    }

}