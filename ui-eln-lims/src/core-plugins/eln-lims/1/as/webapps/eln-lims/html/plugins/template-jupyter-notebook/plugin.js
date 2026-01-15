function CustomJupyterNotebook() {
    this.init();
}

$.extend(CustomJupyterNotebook.prototype, ELNLIMSPlugin.prototype, {
    init: function () {

    },
    sampleTypeDefinitionsExtension: {
        "SHOW_ON_NAV": true,
        "SHOW": false,
        "CUSTOM_SAMPLE_TYPE": {
            extraToolbar : function(mode, sample) {
                var $button = FormUtil.getToolbarButton("JUPYTER");
                $button.append("Custom Jupyter Notebook");
                if(mode == FormMode.VIEW) {
                    $button.click(function() {
                        fetch('/openbis-test/webapp/eln-lims/plugins/template-jupyter-notebook/custom_python_notebook_cells.ipynb').then(r => r.json()).then(customContent => {
                            var jupyterNotebook = new JupyterNotebookController(sample, customContent);
                            jupyterNotebook.init();
                        });
                    });
                } else {
                    $button.attr("disabled", "disabled");
                    $button.click(function() {
                        alert("Function not available during creation.");
                    });
                }

                return {
                    tooltip: "Custom Jupyter Notebook as button",
                    component: $button
                }
            },
            extraToolbarDropdown : function(mode, sample) {
                return {
                    label:"Custom Jupyter Notebook",
                    title:"Custom Jupyter Notebook",
                    action : function() {
                        if(mode == FormMode.VIEW) {
                            fetch('/openbis-test/webapp/eln-lims/plugins/template-jupyter-notebook/custom_python_notebook_cells.ipynb').then(r => r.json()).then(customContent => {
                                var jupyterNotebook = new JupyterNotebookController(sample, customContent);
                                jupyterNotebook.init();
                            });
                        } else {
                            alert("Function not available during creation.");
                        }

                        // var customContent = [];
                        // customContent.push(JupyterUtil.getMarkdownCell("# Custom Title\n"));
                        // customContent.push(JupyterUtil.getMarkdownCell("Custom Paragraph"));
                        // customContent.push(JupyterUtil.getCodeCell(["custom_variable='value'"], "custom_variable"));
                        // var jupyterNotebook = new JupyterNotebookController(sample, customContent);
                        // jupyterNotebook.init();
                    }
                }
            }
        },
    },
});

profile.plugins.push( new CustomJupyterNotebook());