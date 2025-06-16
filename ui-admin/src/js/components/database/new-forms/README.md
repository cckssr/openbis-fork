This Form should cover editing and manipulating all entities from all types.

Space
Project
Experiment
Sample
DataSet // If we only want to support AFS we might not implement DataSet, do last.

5 DTOS

To not deal with 5 diferent models

Adaptor to a single model, we can call this adaptor Form

The next model is work in progress, don't take it as gospel.

1 DTO

A Form represents an Entity to be edited or manipulated in some way.

enum FormMode { view, new, edit }
enum DataType { openbis-data-types + word-processor + spreadsheet }

Form
	- entityId	<- permId
	- title
	- List<Field>
	- meta

FormField
	- formFieldTypeId <- PropertyType
	- Value
	- DataType
	- meta

But somewhere we need to build this adaptor DTO from the actual Entity, EntityKind and EntityType

We will encapsulate entity kind and type specific logic into the controller of the form.

Every rendered Field should be updating its FormField model as the user fill it.

EntityForm(Form form, FormMode mode, FormController controller)

private Form form;
private FormMode mode;
private FormController controller;
private List<Button> toolbar;

load() {
	This can happen outside of the main thread to load anything necessary
	controller.load(form, mode);
}

render() {
}

save() {
	controller.save(form, mode);
}

edit() {
	controller.edit(form, mode);
}

delete() {
	controller.delete(form);
	// Leave not implemented
}

move() {
	controller.move(form);
	// Leave not implemented
}

Source Code Requirements

Import and use only V3 API calls.

Please write all this as Type Script.

Additional Requirements

We should be able to present modal windows to implement delete and move.

We should have an extensible toolbar where the form attaches its buttons and allow to attach others.

Only actions that can be done by the user should attach buttons. For that you need to check the user rights. There is a call on the V3 API for this, is used on the ELN.

Client side, auto-save, maybe once a minute, maybe the user can enable/disable the feature with a slidebar at the top of the form.

Conflict resolution, avoid editing entities of a different version. Provide feed back to the user if properties collide so he can edit it. If properties don't collide it can be resolved silently.

Samples need to also be able to manage parents and children using the current table. Indicate where the Parents/Children widgets are in any part of the form.

DataSet need to be able to upload data but the current upload widget sucks and we want to move to use the AFS, do last, we might drop it.

Indicate help boxes with text and images in any part of the form.

Multi valued properties

Freezing

Other architectural notes

Where to create the Form model to instanciate the Form Engine?

The most natural place is either:
	- The controller of the navigation that will instantiate a generic EntityForm with a specific model. <- More but smaller source files, more layers.
	or
	- The controller of the navigation will instantiate a specific EntityForm without the model that will be loaded inside. <- Less source files, bigger navitation controller.

Let's go with the first option.