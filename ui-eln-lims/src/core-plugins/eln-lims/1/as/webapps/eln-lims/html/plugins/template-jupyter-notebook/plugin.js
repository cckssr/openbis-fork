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
            extraToolbarDropdown : function(mode, sample) {
                return {
                    label:"Custom Jupyter Notebook",
                    title:"Custom Jupyter Notebook",
                    action : function() {
                        var customContent = [];
                        customContent.push(JupyterUtil.getMarkdownCell("# Custom Title\n"));
                        customContent.push(JupyterUtil.getMarkdownCell("Custom Paragraph"));
                        customContent.push(JupyterUtil.getCodeCell(["custom_variable='value'"], "custom_variable"));
                        var jupyterNotebook = new JupyterNotebookController(sample, customContent);
                        jupyterNotebook.init();
                    }
                }
            }
        },
    },
});

profile.plugins.push( new CustomJupyterNotebook());