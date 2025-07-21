import { ActionContext, FormMode } from "@src/js/components/database/new-forms/types/form.types.ts";

export class CoreFormModel {
	static editAction = (context: ActionContext) => {
		context.setMode(FormMode.EDIT);
	};
	
	static cancelEditAction = (context: ActionContext) => {
		context.setMode(FormMode.VIEW);
		// The logic to reset the form state is now handled in the context provider
	};
	
	static cancelNewFormAction = (context: ActionContext) => {
		context.closeForm();
		// The logic to reset the form state is now handled in the context provider
	};
	
	static autoSaveAction = (context: ActionContext) => {
		context.setAutoSaveEnabled(!context.isAutoSaveEnabled);
	};
}