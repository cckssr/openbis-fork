import { IAutoSaveActionContext, IModeActionContext, IExtendedActionContext } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { FormMode } from "@src/js/components/database/new-forms/types/formEnums.ts";

export class CoreFormModel {
	static editAction = (context: IModeActionContext) => {
		context.setMode(FormMode.EDIT);
	};
	
	static cancelEditAction = (context: IModeActionContext) => {
		context.setMode(FormMode.VIEW);
	};
	
	static cancelNewFormAction = (context: IExtendedActionContext) => {
		context.externalAppController.closeForm(context.form.entityType, context.form.entityPermId);
	};
	
	static autoSaveAction = (context: IAutoSaveActionContext) => {
		context.setAutoSaveEnabled(!context.isAutoSaveEnabled);
	};

	static deleteAction = (context: IExtendedActionContext) => {
		context.controller.delete(context.form, context);
	};

	static unknownAction = (actionName: string) => {
		alert(`Unknown action: ${actionName}`);
	};

	static moveAction = (context: IExtendedActionContext) => {
		context.controller.move(context.form);
	};
}