function CustomJupyterNotebook() {
    this.init();
}

$.extend(CustomJupyterNotebook.prototype, ELNLIMSPlugin.prototype, {
    init: function () {

    },
    sampleTypeDefinitionsExtension: {
        "CUSTOM_SAMPLE_TYPE": {
            extraToolbarDropdown : function(mode, sample) {
                return {
                    label:"Custom Jupyter Notebook",
                    title:"Custom Jupyter Notebook",
                    action : function() {
                        alert("Test");
                    }
                }
            }
        },
    },
});

profile.plugins.push( new CustomJupyterNotebook());