function MoveEntityView(moveEntityController, moveEntityModel) {

    var $entityBox = $("<div>");
    var $optionalBox = $("<div>");

	this.repaint = function() {
		var $window = $('<form>', { 'action' : 'javascript:void(0);' });

		$window.submit(function() {
			Util.unblockUI();
            var descendants = false;
            var inputs = $window.find("input");
            if (inputs.length > 0) {
                descendants = inputs[0].checked;
            }
			moveEntityController.move(descendants);
		});

		var onlyOne = moveEntityModel.entities.length == 1;
        if(onlyOne) {
            var identifier, type, owningEntityType, owningEntityIdentifier;
            switch(moveEntityModel.entity["@type"]) {
                case "as.dto.experiment.Experiment":
                    identifier = moveEntityModel.entity.getIdentifier();
                    type = moveEntityModel.entity.getType().getCode();
                    owningEntityType = ELNDictionary.Project;
                    owningEntityIdentifier = moveEntityModel.entity.getProject().getIdentifier().identifier;
                    break;
                case "as.dto.sample.Sample":
                    identifier = moveEntityModel.entity.getIdentifier();
                    type = moveEntityModel.entity.getType().getCode();
                    if(moveEntityModel.entity.getExperiment()) {
                        owningEntityType = ELNDictionary.getExperimentDualName();
                        owningEntityIdentifier = moveEntityModel.entity.getExperiment().getIdentifier().identifier;
                    } else if(moveEntityModel.entity.getProject()) {
                        owningEntityType = ELNDictionary.Project;
                        owningEntityIdentifier = moveEntityModel.entity.getProject().getIdentifier().identifier;
                    } else {
                        owningEntityType = ELNDictionary.Space;
                        owningEntityIdentifier = moveEntityModel.entity.getSpace().getCode();
                    }
                    break;
                case "as.dto.project.Project":
                    identifier = moveEntityModel.entity.getIdentifier();
                    type = ELNDictionary.Project;
                    owningEntityType = ELNDictionary.Space;
                    owningEntityIdentifier = moveEntityModel.entity.getSpace().getCode();
                    break;
                case "as.dto.dataset.DataSet":
                    identifier = moveEntityModel.entity.getPermId();
                    type = moveEntityModel.entity.getType().getCode();
                    if(moveEntityModel.entity.getSample()) {
                        owningEntityType = ELNDictionary.sample;
                        owningEntityIdentifier = moveEntityModel.entity.getSample().getIdentifier().identifier;
                    } else {
                        owningEntityType = ELNDictionary.getExperimentDualName();
                        owningEntityIdentifier = moveEntityModel.entity.getExperiment().getIdentifier().identifier;
                    }
                    break;
            }
            $window.append($('<legend>').append("Moving " + identifier));

            $window.append(FormUtil.getFieldForLabelWithText("Type", type));
            $window.append(FormUtil.getFieldForLabelWithText("Current " + owningEntityType.toLowerCase(), owningEntityIdentifier));

		} else {
		    var count = moveEntityModel.entities.length;
		    var header = "Moving (" + count + ") ";
		    switch(moveEntityModel.entity["@type"]) {
                case "as.dto.experiment.Experiment":
                     $window.append($('<legend>').append(header + ELNDictionary.ExperimentsCollection.toLowerCase() + " to:"));
                    break;
                case "as.dto.sample.Sample":
                    $window.append($('<legend>').append(header + ELNDictionary.samples + " to:"));
                    break;
                case "as.dto.project.Project":
                    $window.append($('<legend>').append(header + ELNDictionary.Projects.toLowerCase() + " to:"));
                    break;
                case "as.dto.dataset.DataSet":
                    $window.append($('<legend>').append(header + ELNDictionary.Datasets.toLowerCase() + " to:"));
                    break;
            }

		}

        $window.append($optionalBox);
        if(moveEntityModel.entity["@type"] === "as.dto.sample.Sample") {
            var _this = this;
            $window.append(FormUtil.getFieldForComponentWithLabel(FormUtil.getOptionsRadioButtons("oldOrNewExp",true, ["Existing Space, Project or Experiment/Collection", "New " + ELNDictionary.getExperimentDualName() + ""], function(event) {
                var value = $(event.target).val();
                if(value === "Existing entity") {
                    moveEntityModel.isNewExperiment = false;
                    _this.repaintExistingEntity();
                } else {
                    moveEntityModel.isNewExperiment = true;
                    _this.repaintNewEntity();
                }
            }), "Choose existing Space, Project or Experiment/Collection or create a new " + ELNDictionary.getExperimentDualName() +"?"));
        } else {
            $window.append($('<br>'));
        }

		$window.append($entityBox);
		this.repaintExistingEntity();

        var $btnAccept = $('<input>', { 'type': 'submit', 'class' : 'btn btn-primary', 'value' : 'Accept' });
        var $btnCancel = $('<a>', { 'class' : 'btn btn-default' }).append('Cancel');
        $btnCancel.click(function() {
            Util.unblockUI();
        });

        $window.append('<br>').append($btnAccept).append('&nbsp;').append($btnCancel);

		
		var css = {
				'text-align' : 'left',
				'top' : '15%',
				'width' : '70%',
				'left' : '15%',
				'right' : '20%',
				'overflow' : 'hidden'
		};
		
		Util.blockUI($window, css);
	}

	this.repaintNewEntity = function() {
        var _this = this;
        $entityBox.empty();
        FormUtil.getProjectAndExperimentsDropdown(true, false, true, function($dropdown) {
            $dropdown.attr("id", "future-projects-drop-down");
            //Fields
            var $expTypeField = FormUtil.getExperimentTypeDropdown("future-experiment-type-drop-down", true);
            var $expNameField = FormUtil._getInputField('text', null, 'Future ' + ELNDictionary.getExperimentDualName() + ' Name', null, true);

            //Events
            var newExpEvent = function(event){
                $expNameField.val($expNameField.val().toUpperCase());
                var projectIdentifier = $dropdown.val();
                var projectSpace = IdentifierUtil.getSpaceCodeFromIdentifier(projectIdentifier);
                var projectCode = IdentifierUtil.getCodeFromIdentifier(projectIdentifier);
                var experimentCode = $expNameField.val();
                moveEntityModel.experimentIdentifier = IdentifierUtil.getExperimentIdentifier(projectSpace, projectCode, experimentCode);
            };
            var newTypeEVent = function(event) {
                var value = $(event.target).val();
                moveEntityModel.experimentType = value;
            };

            //Attach Events
            $dropdown.change(newExpEvent);
            $expNameField.keyup(newExpEvent);
            $expTypeField.change(newTypeEVent);

            //Attach Fields
            $entityBox.append(FormUtil.getFieldForComponentWithLabel($dropdown, "Future Project"))
                    .append(FormUtil.getFieldForComponentWithLabel($expTypeField, "Future " + ELNDictionary.getExperimentDualName() + " Type"))
                    .append(FormUtil.getFieldForComponentWithLabel($expNameField, "Future " + ELNDictionary.getExperimentDualName() + " Code"));
        });
    }

    this.repaintExistingEntity = function() {
        var _this = this;
        $entityBox.empty();
        $optionalBox.empty();
        var advancedEntitySearchDropdown = null;

        switch(moveEntityModel.entity["@type"]) {
            case "as.dto.experiment.Experiment":
                advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(false, true, "search entity to move to",
                        false, false, false, true, false);
                break;
            case "as.dto.sample.Sample":
                $optionalBox.append(FormUtil.getFieldForComponentWithLabel(FormUtil._getBooleanField("move_descendants"),
                        "Click the checkbox if also all descendant " + ELNDictionary.sample
                        + "s (i.e. children, grand children etc.) including their data sets should be moved. "
                        + "Only those descendants are moved which belong to the same entity"
                        + " as this " + ELNDictionary.sample, null, true))
                advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(false, true, "search entity to move to",
                        true, false, false, true, true);
                break;
            case "as.dto.project.Project":
                advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(false, true, "search entity to move to",
                        false, false, false, false, true);
                break;
            case "as.dto.dataset.DataSet":
                advancedEntitySearchDropdown = new AdvancedEntitySearchDropdown(false, true, "search entity to move to",
                        true, true, false, false, false);
                break;
        }

        advancedEntitySearchDropdown.onChange(function(selected) {
            moveEntityModel.selected = selected[0];
        });

        advancedEntitySearchDropdown.init($entityBox);
    }
}