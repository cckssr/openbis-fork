
var JExcelEditorManager = new function() {
    this.jExcelEditors = {}

    this.getOnChange = function(guid, propertyCode, entity) {
        var _this = this;
        return function(el, record, x, y, value) {
            var jExcelEditor = _this.jExcelEditors[guid];
            if(jExcelEditor) {
//                if(value) {
//                    // Change column width
//                    var columnWidth = parseInt(jExcelEditor.getWidth(x));
//                    var td = el.children[1].children[0].children[2].children[y].children[parseInt(x)+1];
//                    var columnScrollWidth = td.scrollWidth;
//
//                    if(columnScrollWidth > columnWidth) {
//                        jExcelEditor.setWidth(x, columnScrollWidth + 10);
//                    }
//                }
                // Save Editor
                var headers = jExcelEditor.getHeaders(true);
                var data = jExcelEditor.getData();
                var values = jExcelEditor.getData();
                // little hack because jExcelEditor.getData(false, true) is not returning processed results
                for(let rowIndex in values) {
                    values[rowIndex] = Object.values(values[rowIndex]).map((val, index) => {
                        if(_this._isString(val) && val.startsWith('=')) {
                            var row = parseInt(rowIndex)+1;
                            return jExcelEditor.getValue(headers[index] + row, true);
                        }
                        return val;
                    });
                }
                var style = jExcelEditor.getStyle();
                var meta = jExcelEditor.getMeta();
                var width = jExcelEditor.getWidth();
                var jExcelEditorValue = {
                    headers : headers,
                    data : data,
                    style : style,
                    meta : meta,
                    width : width,
                    values : values
                }
                var text = window.unescape(window.encodeURIComponent(JSON.stringify(jExcelEditorValue)));
                // force utf-8 encoding using TextEncoder
                var arr = new TextEncoder().encode(text);
                text = new TextDecoder("utf8").decode(arr);

                 entity.properties[propertyCode] = "<DATA>" + window.btoa(text) + "</DATA>";
            }
        }
    }

    this.getObjectFunction = function(guid) {
        var _this = this;
        return function() {
            var jExcelEditor = _this.jExcelEditors[guid];
            var x = null;
            var y = null;
            if(jExcelEditor.selectedCell) {
                x = parseInt(jExcelEditor.selectedCell[0]);
                y = parseInt(jExcelEditor.selectedCell[1]);
            } else {
                Util.showInfo("Select a cell first.");
                return;
            }

            var component = "<div>"
                component += "<legend>Insert Object</legend>";
            	component += "<div>";

                component += "<div class='form-group'>";
                component += "<label class='control-label'>Object:</label>";
                component += "<div>";
                component += "<div id='objectSelector'></div>";
                component += "</div>";
                component += "</div>";

                component += "<div class='form-group'>";
                component += "<label class='control-label'>Options:</label>";
                component += "<div class='controls'>";
                component += "<span class='checkbox'><label><input type='checkbox' id='insertHeaders'> Insert Headers </label></span>";
                component += "</div>";
                component += "</div>";

                component += "</div>";
                component += "</div>";
                Util.blockUI(component + "<a class='btn btn-default' id='insertAccept'>Accept</a> <a class='btn btn-default' id='insertCancel'>Cancel</a>", FormUtil.getDialogCss());

                var advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(true, true, "Select Object", false, true, false, false, false);
                advancedEntitySearchDropdown.init($("#objectSelector"));

                $("#insertCancel").on("click", function(event) {
                    Util.unblockUI();
                });

                $("#insertAccept").on("click", function(event) {
                    var insertHeaders = $("#insertHeaders")[0].checked;
                    var selected = advancedEntitySearchDropdown.getSelected();
                    var lastEntityKindType = null;

                    if(selected.length > 0) {
                        for(var sIdx = 0; sIdx < selected.length; sIdx++) {
                            var entity = selected[sIdx];
                            var entityKindType = entity["@type"] + ":" + entity.type.code;
                            var entityTable = _this.getEntityAsTable(entity);
                            var columnCount = jExcelEditor.getHeaders().split(',').length;
                            var rowCount = jExcelEditor.getData().length;
                            if(insertHeaders && lastEntityKindType !== entityKindType) {
                                //Insert Labels
                                for(var lIdx = 0; lIdx < entityTable.label.length; lIdx++) {
                                    var label = entityTable.label[lIdx];
                                    if(label) {
                                        for(;columnCount <= x+lIdx; columnCount++) {
                                            jExcelEditor.insertColumn();
                                        }
                                        for(;rowCount <= y;rowCount++) {
                                            jExcelEditor.insertRow();
                                        }
                                        jExcelEditor.setValueFromCoords(x+lIdx, y, label, true);
                                    }
                                }
                                y++;
                            }

                            //Insert Values
                            for(var vIdx = 0; vIdx < entityTable.value.length; vIdx++) {
                                var value = entityTable.value[vIdx];
                                if(value) {
                                    for(;columnCount <= x+vIdx; columnCount++) {
                                        jExcelEditor.insertColumn();
                                    }
                                    for(;rowCount <= y;rowCount++) {
                                        jExcelEditor.insertRow();
                                    }
                                    jExcelEditor.setValueFromCoords(x+vIdx, y, value, true);
                                }
                            }
                            y++;
                            lastEntityKindType = entityKindType;
                        }
                        Util.unblockUI();
                    } else {
                        Util.showInfo("Select an object first.", function() {}, true);
                    }
                });
        }
    }

	this.createField = function($container, mode, propertyCode, entity) {
	    $container.attr('style', 'width: 100%; height: 450px; overflow-y: scroll; overflow-x: scroll;');
	    var _this = this;

        var headers = null;
	    var data = [];
	    var style = null;
	    var meta = null;
        var width = null;
	    if(entity && entity.properties && entity.properties[propertyCode]) {
	        var jExcelEditorValueAsStringWithTags = entity.properties[propertyCode];
	        var jExcelEditorValue = null;
	        if(jExcelEditorValueAsStringWithTags) {
	            var jExcelEditorValueAsStringNoTags = jExcelEditorValueAsStringWithTags.substring(6, jExcelEditorValueAsStringWithTags.length - 7);
                try {
                    // Improved decoding, used for the new encoding
                    jExcelEditorValue = JSON.parse(window.decodeURIComponent(window.escape(window.atob(jExcelEditorValueAsStringNoTags))));
                } catch (error) {
                    // Original decoding, used to support systems until they update
                    jExcelEditorValue = JSON.parse(window.atob(jExcelEditorValueAsStringNoTags));
                }
	        }
	        if(jExcelEditorValue) {
	            headers = jExcelEditorValue.headers;
	            data = jExcelEditorValue.data;
	            style = jExcelEditorValue.style;
	            meta = jExcelEditorValue.meta;
	            width = jExcelEditorValue.width;
	        }
	    }

        var guid = Util.guid();

        var options = {
                    data: data,
                    style: style,
                    meta: meta,
                    editable : mode !== FormMode.VIEW,
                    minDimensions:[10, 10],
                    toolbar: null,
                    onchangeheader: null,
                    onchange: null,
                    onchangestyle: null,
                    onchangemeta: null
        };

        if(headers) {
            options.colHeaders = headers;
        }

        if(width) {
            options.colWidths = width;
        }

        if(mode === FormMode.VIEW) {
            options.allowInsertRow = false;
            options.allowManualInsertRow = false;
            options.allowInsertColumn = false;
            options.allowManualInsertColumn = false;
            options.allowDeleteRow = false;
            options.allowDeleteColumn = false;
            options.allowRenameColumn = false;
            options.allowComments = false;
            options.onload = function(container,spreadsheet) {
                                    var data = spreadsheet.getData();
                                    var headers = spreadsheet.getHeaders().split(',');
                                    var cellsWithIdentifiers = [];
                                    var identifiers = new Set();
                                    for (let i=0; i<data.length; i++ ) {
                                        for(let j=0; j < data[i].length; j++) {
                                            let cellData = data[i][j];
                                            if(_this._isIdentifierCell(cellData)) {
                                                let cellIndex = headers[j] + (i+1);
                                                let cell = spreadsheet.getCell(cellIndex);
                                                cellsWithIdentifiers.push({cell: cell, cellData: cellData});
                                                cellData.split(/\s+/)
                                                    .filter(_this._isIdentifier)
                                                    .forEach(id => identifiers.add(id));
                                                cell.innerHTML = Util.getProgressBarSVG();
                                            }
                                        }
                                    }
                                    _this._searchByIdentifiers(Array.from(identifiers), function(results) {

                                        for(cell of cellsWithIdentifiers) {
                                            var stringArray = cell.cellData.split(/(\s+)/);
                                            var cellText = "";
                                            for (let word of stringArray) {
                                                if(results[word]) {
                                                    cellText += results[word][0].outerHTML;
                                                } else {
                                                    cellText += word;
                                                }
                                            }
                                            cell.cell.innerHTML = cellText;

                                            cell.cell.onclick = function(event) {
                                                results[event.target.innerText].click();
                                            }
                                        }

                                    });
                                }

            options.contextMenu = function(obj, x, y, e) {
                return [];
            }
        } else {
            var onChangeHandler = this.getOnChange(guid, propertyCode, entity);
            options.onundo = onChangeHandler;
            options.onredo = onChangeHandler;
            options.onchange = onChangeHandler; //
            options.onafterchanges = onChangeHandler;
            options.oninsertrow = onChangeHandler;
            options.oninsertcolumn = onChangeHandler;
            options.ondeleterow = onChangeHandler;
            options.ondeletecolumn = onChangeHandler;
            options.onmoverow = onChangeHandler;
            options.onmovecolumn = onChangeHandler;
            options.onresizerow = onChangeHandler;
            options.onresizecolumn = onChangeHandler;
            options.onsort = onChangeHandler;
            options.onpaste = onChangeHandler;
            options.onmerge = onChangeHandler;
            options.onchangeheader = onChangeHandler; //
            options.oneditionend = onChangeHandler;
            options.onchangestyle = onChangeHandler; //
            options.onchangemeta = onChangeHandler; //

            options.toolbar = [
                    { type:'select', k:'font-family', v:['Arial','Verdana'] },
                    { type:'select', k:'font-size', v:['9px','10px','11px','12px','13px','14px','15px','16px','17px','18px','19px','20px'] },
                    { type:'i', content:'format_align_left', k:'text-align', v:'left' },
                    { type:'i', content:'format_align_center', k:'text-align', v:'center' },
                    { type:'i', content:'format_align_right', k:'text-align', v:'right' },
                    { type:'i', content:'format_bold', k:'font-weight', v:'bold' },
                    { type:'color', content:'format_color_text', k:'color' },
                    { type:'color', content:'format_color_fill', k:'background-color' },
                    { type:'i', content:'input', onclick: this.getObjectFunction(guid) },
            ];
        }

        var jexcelField = jspreadsheet($container[0], options);

        $container.refresh = function() {
            $container.empty();
            jexcelField.init();
        }

        this.jExcelEditors[guid] = jexcelField;
	}

	this._isIdentifierCell = function(cellData) {
	    if(!this._isString(cellData) || cellData == '') {
	        return false;
	    }
	    var arr = cellData.split(/\s+/).filter(Boolean);
        for(let element of cellData.split(/\s+/).filter(Boolean)) {
            if(this._isIdentifier(element)) {
                return true;
            }
        }
        return false;
	}

	this._isString = function(value) {
        return typeof value === 'string' || value instanceof String;
	}

	this._isIdentifier = function(data) {
        var split = data.split('/');
        return split[0] == '' && (split.length > 2 && split.length < 6);
	}

	this._searchByIdentifiers = function(identifiers, callback) {

	    require([ "as/dto/sample/id/SampleIdentifier", "as/dto/sample/fetchoptions/SampleFetchOptions",
	     "as/dto/experiment/id/ExperimentIdentifier", "as/dto/experiment/fetchoptions/ExperimentFetchOptions"],
                    function(SampleIdentifier, SampleFetchOptions, ExperimentIdentifier, ExperimentFetchOptions) {

                        var sampleFetchOptions = new SampleFetchOptions();

                        var ids = identifiers.map(id => new SampleIdentifier(id));

                        mainController.openbisV3.getSamples(ids, sampleFetchOptions).done(function(sampleResults) {
                            let links = {};
                            let missing = []
                            for(let id of identifiers) {
                                if(sampleResults[id]) {
                                    let sample = sampleResults[id];
                                    links[id] = FormUtil.getFormLink(sample.identifier.identifier, 'Sample', sample.permId.permId, null);
                                } else {
                                    missing.push(id);
                                }
                            }
                            if(missing.length == 0) {
                                callback(links);
                            } else {
                                var experimentFetchOptions = new ExperimentFetchOptions();
                                var ids = missing.map(id => new ExperimentIdentifier(id));
                                mainController.openbisV3.getExperiments(ids, experimentFetchOptions).done(function(experimentResults) {
                                    var experiments = Util.mapValuesToList(experimentResults);
                                    for ( let experiment of experiments) {
                                        links[experiment.identifier.identifier] = FormUtil.getFormLink(experiment.identifier.identifier, 'Experiment', experiment.identifier.identifier, null);
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

	this.getEntityAsTable = function(entity) {
	    var tableModel = {
	        code : [],
	        label : [],
	        value : [],
	        dataType : [],
	    }

	    if(entity["@type"] === "as.dto.sample.Sample") {
            var sampleType = profile.getSampleTypeForSampleTypeCode(entity.type.code);

            tableModel.code.push("");
            tableModel.label.push("Identifier");
            tableModel.value.push(entity.identifier.identifier);
            tableModel.dataType.push("");

            for(var i = 0; i < sampleType.propertyTypeGroups.length; i++) {
                var propertyGroup = sampleType.propertyTypeGroups[i].propertyTypes;
                for(var j = 0; j < propertyGroup.length; j++) {
            	    var propertyType = propertyGroup[j];
            	    tableModel.code.push(propertyType.code);
            	    tableModel.label.push(propertyType.label);
            	    tableModel.value.push(entity.properties[propertyType.code]);
            	    tableModel.dataType.push(propertyType.dataType);
                }
            }
	    }
	    return tableModel;
	}
}