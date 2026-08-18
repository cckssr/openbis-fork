import { IAutoSaveActionContext, IModeActionContext, IExtendedActionContext } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { EntityKind, FormMode } from "@src/js/components/database/new-forms/types/formEnums.ts";
import { findFormFieldById } from "@src/js/components/database/new-forms/utils/formFieldUtil.ts";

export class CoreFormModel {

	static mapNewToEntityKind = (entityKind: string) => {
		switch (entityKind) {
			case EntityKind.NEW_COLLECTION:
				return EntityKind.COLLECTION;
			case EntityKind.NEW_PROJECT:
				return EntityKind.PROJECT;
			case EntityKind.NEW_OBJECT:
				return EntityKind.OBJECT;
			case EntityKind.NEW_DATASET:
				return EntityKind.DATASET;
			default:
				return entityKind;
		}
	}

	static saveAction = async (context: IExtendedActionContext) => {
		const { form, controller, onAfterSave, mode } = context;
		await new Promise(resolve => setTimeout(resolve, 500));
		const newPermId = await controller.save(form, mode);
		console.log("CoreFormModel.saveAction: saved successfully! New permId:", newPermId);
		if (mode === FormMode.CREATE) {
			const oldId = form.entityPermId.substring(0, form.entityPermId.indexOf('-'));
			onAfterSave({
				oldType: form.entityKind,
				oldId: oldId,
				newType: CoreFormModel.mapNewToEntityKind(form.entityKind),
				newId: newPermId
			});
		} else {
			onAfterSave();
		}
	};

	static editAction = (context: IModeActionContext) => {
		context.setMode(FormMode.EDIT);
	};

	static cancelEditAction = async (context: IExtendedActionContext) => {
		// Reload the form to restore original values
		const performCancel = async () => {
			try {
				const originalForm = await context.controller.load(context.form.entityPermId);
				context.setForm(originalForm);
			} finally {
				context.setMode(FormMode.VIEW);
			}
		};
		if (!context.isAutoSaveEnabled && context.requestUnsavedConfirmation) {
			context.requestUnsavedConfirmation(performCancel);
		} else {
			await performCancel();
		}
	};

	static cancelNewFormAction = (context: IExtendedActionContext) => {
		const performCancel = () => {
			const originalId = context.form.entityPermId.substring(0, context.form.entityPermId.indexOf('-'));
			const params = {
				type: context.form.entityKind,
				id: originalId
			};
			context.externalAppController.closeForm(params);
		};
		if (!context.isAutoSaveEnabled && context.requestUnsavedConfirmation) {
			context.requestUnsavedConfirmation(performCancel);
		} else {
			performCancel();
		}
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

	/* 	static newEntityAction = (context: IExtendedActionContext, selectedEntityType: string) => {
			const fromId = findFormFieldById(context.form.fields, context.form.entityPermId, 'identifier', true) as string;
			const fromObjectType = CoreFormModel.mapNewToEntityKind(context.form.entityKind);
			if (context.externalAppController) {
				context.externalAppController.createNewObject({ newObjectType: EntityKind.NEW_ENTITY, fromObjectType: context.form.entityKind, fromId: fromId, selectedEntityType: selectedEntityType });
			} else {
				console.warn("onNewEntity callback not provided to context.");
				throw new Error("onNewEntity callback not provided to context.");
			}
		}; */

	static newCollectionAction = (context: IExtendedActionContext, selectedEntityType: string) => {
		const { form, externalAppController } = context;
		if (externalAppController) {
			externalAppController.createNewObject({ 
				newObjectType: EntityKind.NEW_COLLECTION, 
				fromObjectType: form.entityKind, 
				fromId: form.entityPermId, 
				selectedEntityType: selectedEntityType 
			});
		} else {
			console.warn("onNewCollection callback not provided to context.");
			throw new Error("onNewCollection callback not provided to context.");
		}
	};

	static newObjectAction = (context: IExtendedActionContext, selectedEntityType: string) => {
		const { form, externalAppController } = context;
		const sourceEntityKind = form.entityKind;
		let sourceEntityId = null;
		if (sourceEntityKind === EntityKind.SPACE) {
			sourceEntityId = findFormFieldById(form.fields, form.entityPermId, 'code', true) as string;
		} else {
			sourceEntityId = form.entityPermId;
		}
		if (externalAppController) {
			externalAppController.createNewObject({ 
				newObjectType: EntityKind.NEW_OBJECT, 
				fromObjectType: sourceEntityKind, 
				fromId: sourceEntityId, 
				selectedEntityType: selectedEntityType 
			});
		} else {
			console.warn("onNewObject callback not provided to context.");
			throw new Error("onNewObject callback not provided to context.");
		}
	};

	static newDatasetAction = (context: IExtendedActionContext, selectedEntityType: string) => {
		const { form, externalAppController } = context;
		if (externalAppController) {
			externalAppController.createNewObject({ 
				newObjectType: EntityKind.NEW_DATASET, 
				fromObjectType: form.entityKind, 
				fromId: form.entityPermId, 
				selectedEntityType: selectedEntityType 
			});
		} else {
			console.warn("onNewDataset callback not provided to context.");
			throw new Error("onNewDataset callback not provided to context.");
		}	
	};
}