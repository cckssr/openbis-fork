import { ActionContext } from "@src/js/components/database/new-forms/types/form.types.ts";
import { FormMode } from "@src/js/components/database/new-forms/types/form.enums.ts";

export class CoreFormModel {
	static editAction = (context: ActionContext) => {
		context.setMode(FormMode.EDIT);
	};
	
	static cancelEditAction = (context: ActionContext) => {
		context.setMode(FormMode.VIEW);
		// The logic to reset the form state is now handled in the context provider
	};
	
	static cancelNewFormAction = (context: ActionContext) => {
		context.externalAppController.closeForm();
		// The logic to reset the form state is now handled in the context provider
	};
	
	static autoSaveAction = (context: ActionContext) => {
		context.setAutoSaveEnabled(!context.isAutoSaveEnabled);
	};

	static deleteAction = (context: ActionContext) => {
		context.controller.delete(context.form, context);
	};

	static unknownAction = (actionName: string) => {
		alert(`Unknown action: ${actionName}`);
	};
}