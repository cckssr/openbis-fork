/*
 * Copyright 2026 ETH Zuerich, Scientific IT Services
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
function RoCrateExportView(exportController, exportModel) {
    var _refreshableFields = [];
    var _this = this;

    this.exportModel = exportModel;
    this.exportController = exportController;
    this._viewId = mainController.getNextId();


    this.refresh = function() {
        for(var field of _refreshableFields) {
            field.refresh();
        }
    }

    this.repaint = function(views) {
            var $header = views.header;
            var $container = views.content;

            var $form = $("<div>");
            var $formColumn = $("<form>", {
                'name': 'roCrateExportForm',
                'role': 'form',
                'action': 'javascript:void(0);',
                'onsubmit': 'mainController.currentView.exportSelected();'
            });
            $form.append($formColumn);


            var $options = $("<div>");
            var $compatible = $("<span class='checkbox'><label><input type='checkbox' checked id='COMPATIBLE-IMPORT-"+_this._viewId+"' disabled>Make import compatible</label></span>");
            $options.append($compatible);

            $options.append($("<legend>").append("File formats"));

            var $pdf = $("<span class='checkbox'><label><input type='checkbox' checked id='PDF-EXPORT-"+_this._viewId+"'>Export metadata as PDF</label></span>");
            $options.append($pdf);
            var $xlsx = $("<span class='checkbox'><label><input type='checkbox' checked id='XLSX-EXPORT-"+_this._viewId+"'>Export metadata as XLSX</label></span>");
            $options.append($xlsx);
            var $data = $("<span class='checkbox'><label><input type='checkbox' checked id='DATASET-EXPORT-"+_this._viewId+"'>Export dataset data</label></span>");
            $options.append($data);
            var $data = $("<span class='checkbox'><label><input type='checkbox' checked id='FILES-EXPORT-"+_this._viewId+"'>Export files</label></span>");
            $options.append($data);


            var $hierarchyInclusions = $("<legend>").append("Hierarchy Inclusions (Same Space)");

            $options.append($hierarchyInclusions);

            var $withLevelsAbove = $("<span class='checkbox'><label><input type='checkbox' id='LEVELS-ABOVE-EXPORT' checked disabled>Include levels above (Always included)</label></span>");
            $options.append($withLevelsAbove);
            var $includeParents = $("</span><span class='checkbox'><label><input type='checkbox' id='PARENTS-EXPORT-"+_this._viewId+"'>Include Object and Dataset parents</label></span>").css({ "padding-left" : "20px" });
            $options.append($includeParents);

            var $levelsBelow = $("<span class='checkbox'><label><input type='checkbox' id='LEVELS-BELOW-EXPORT-"+_this._viewId+"'>Include levels below</label></span>");
            $options.append($levelsBelow);
            var $includeChildren = $("<span class='checkbox'><label><input type='checkbox' id='CHILDREN-EXPORT-"+_this._viewId+"' disabled>Include Object and Dataset children</label></span>").css({ "padding-left" : "20px" });
            $options.append($includeChildren);

            var levelsBelowChange = function(event) {
                $("#CHILDREN-EXPORT-"+_this._viewId)[0].disabled = !event.target.checked;
                if (!event.target.checked) {
                    $("#CHILDREN-EXPORT-"+_this._viewId)[0].checked = false;
                    var enabled = $("#PARENTS-EXPORT-"+_this._viewId)[0].checked || $("#CHILDREN-EXPORT-"+_this._viewId)[0].checked;
                    if(!enabled) {
                        $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].disabled = true;
                        $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].checked = false;
                    }
                }
            }
            $levelsBelow.change(levelsBelowChange);
            $levelsBelow.refresh = function() {
                this.off('change')
                this.change(levelsBelowChange);
            }
            _refreshableFields.push($levelsBelow)

            var includeParentsChildrenChangeEvent = function(event) {
                var enabled = $("#PARENTS-EXPORT-"+_this._viewId)[0].checked || $("#CHILDREN-EXPORT-"+_this._viewId)[0].checked;
                $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].disabled = !enabled;
                if (!enabled) {
                    $("#OTHER-SPACES-EXPORT-"+_this._viewId)[0].checked = false;
                }
            }
            $includeParents.change(includeParentsChildrenChangeEvent);
            $includeParents.refresh = function() {
                this.off('change')
                this.change(includeParentsChildrenChangeEvent);
            }
            _refreshableFields.push($includeParents)
            $includeChildren.change(includeParentsChildrenChangeEvent);
            $includeChildren.refresh = function() {
                this.off('change')
                this.change(includeParentsChildrenChangeEvent);
            }
            _refreshableFields.push($includeChildren)



            var $spaceInclusions = $("<legend>").append("Hierarchy Inclusions (Other Spaces)");
            $options.append($spaceInclusions);
            var $includeOtherSpaces = $("<span class='checkbox'><label><input type='checkbox' id='OTHER-SPACES-EXPORT-"+_this._viewId+"' disabled>Include Objects and Datasets parents and children</label></span>");
            $options.append($includeOtherSpaces);


    		$container.append($options);


            var $tree = $('<div>', { 'id' : 'exportsTree-'+_this._viewId });
            $formColumn.append(FormUtil.getBox().append($tree));

            var $entityTree = $("<legend>").append("Inclusion tree");
            $container.append($entityTree);

            $container.append($form);

            this.exportModel.tree = TreeUtil.getCompleteTree($tree);
            this.exportModel.tree.fancytree('getTree').rootNode.children[0].setExpanded();

            this.exportModel.tableModel = ExportUtil.getTableModel();

            $formColumn.refresh = function() {
                $("form[name='roCrateExportForm']").children().remove()
                var $tree = $('<div>', { 'id' : 'exportsTree-'+_this._viewId });
                $("form[name='roCrateExportForm']").append(FormUtil.getBox().append($tree));
                _this.exportModel.tree = TreeUtil.getCompleteTree($tree);
                _this.exportModel.tree.fancytree('getTree').rootNode.children[0].setExpanded();
            }
            _refreshableFields.push($formColumn);


            var $formTitle = $('<h2>').append('RO-Crate Export Builder');
            $header.append($formTitle);

            var $exportButton = $('<input>', { 'type': 'submit', 'class': 'btn btn-primary', 'value': 'Initialize export',
                'onClick': '$("form[name=\'roCrateExportForm\']").submit()'});
            $header.append($exportButton);



            var columns = [ {
                label : 'Job Id',
                property : 'jobId',
                sortable : true
            } , {
                label : 'Current Status',
                property : 'currentStatus',
                render: function(data) {
                    return data.currentStatus;
                },
                sortable : true
            }, {
                label : 'Current Status Time',
                property : 'currentStatusTime',
                render: function(data) {
                    return Util.getFormatedDate(new Date(data.currentStatusTime));
                },
                sortable : true
            }, {
               label : '',
               property : 'deleteButton',
               render: function(data) {
                   var delBtn = FormUtil.getToolbarButton("MINUS");
                   var delFun = function(event) {
                     event.stopPropagation();
                     _this.exportModel.jobs.splice(data.id, 1);
                     _this.exportController.updateJobs();
                     _this.dataGrid.refresh();
                     console.log(data);
                   }
                   delBtn.click(delFun);
                   delBtn.refresh = function() {
                     this.off("click");
                     this.on("click", delFun);
                   }
                   _refreshableFields.push(delBtn);
                   return delBtn;
               },
               sortable : false
            }];

            var getDataList = function(callback) {
                var dataList = [];
                for(var idx = 0; idx < _this.exportModel.jobs.length; idx++) {
                    var job =  _this.exportModel.jobs[idx];
                    dataList.push({
                        id: idx,
                        jobId: job.jobId,
                        currentStatus : job.status,
                        currentStatusTime : job.time
                    });
                }
                callback(dataList);
            }
            var rowClick = function(e) {
                var job = _this.exportModel.jobs[e.data.id];
                if(e.data.currentStatus === "COMPLETED") {
                    var downloadUrl = job.downloadUrl;
                    // TODO download result
//                    window.open(result.result, "_blank");
                    Util.showSuccess("TODO Downloading File");
                } else if(e.data.currentStatus === "FAILED") {
                    Util.showError(job.errors);
                }
            }


            var dataGrid = new DataGridController(null, columns, [], null, getDataList, rowClick, false, "RO_CRATE_MONITOR_OVERVIEW", false, false, 90);
            _this.dataGrid = dataGrid;
            dataGrid.setId("ro-crate-monitor-grid");
            var dataGridContainer = $("<div>");
            dataGrid.init(dataGridContainer);
            var $containerColumn = $("<form>", {
                'role' : "form",
                "action" : "javascript:void(0);",
                "onsubmit" : ""
            });
            var $div = $("<div>");
            var $legend = $("<legend>").append("Export jobs");
            $div.append($legend);

            var $infoBox2 = FormUtil.getInfoBox('Export time constraint', [
                'Resource will be available for export once RO-Crate server finishes processing data.',
            ]);
            $infoBox2.css('border', 'none');
            $div.append($infoBox2);

            var displayCheckButton = _this.exportModel.jobs.length > 0 ? ";" : "none;";

            var $refreshButton = $('<div>', {'class' : 'btn btn-default', 'text' : 'Refresh', 'id' : 'ro-crate-status-btn'});
            $refreshButton.css("margin-left", "3px");
            $refreshButton.css("margin-bottom", "3px");
            if(_this.exportModel.jobs.length == 0) {
                $refreshButton.attr("disabled", "disabled");
            }

            var refreshClick = function() {
               _this.exportController.checkStatues();
            };
            $refreshButton.click(refreshClick);

            $refreshButton.refresh = function() {
                this.off("click");
                this.click(refreshClick);
            }
            _refreshableFields.push($refreshButton);

            $div.append($refreshButton);

            $container.append($div);

            var $subDiv = $("<div>");
            $subDiv.append($containerColumn);
            $div.append($subDiv);
            $containerColumn.append(dataGridContainer);

            $container.append($('<br>'));
        };

}