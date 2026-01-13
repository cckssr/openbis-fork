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
			onAfterSave({
				oldType: form.entityKind,
				oldId: form.entityPermId,
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

	static cancelEditAction = async (context: IModeActionContext) => {
		// Reload the form to restore original values
		try {
			const originalForm = await context.controller.load(context.form.entityPermId);
			context.setForm(originalForm);
		} catch (error: any) {
			throw error;
		} finally {
			context.setMode(FormMode.VIEW);
		}
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
		if (context.externalAppController) {
			context.externalAppController.createNewObject({ newObjectType: EntityKind.NEW_COLLECTION, fromObjectType: context.form.entityKind, fromId: findFormFieldById(context.form.fields, context.form.entityPermId, 'identifier', true) as string, selectedEntityType: selectedEntityType });
		} else {
			console.warn("onNewCollection callback not provided to context.");
			throw new Error("onNewCollection callback not provided to context.");
		}
	};

	static newObjectAction = (context: IExtendedActionContext, selectedEntityType: string) => {
		if (context.externalAppController) {
			context.externalAppController.createNewObject({ newObjectType: EntityKind.NEW_OBJECT, fromObjectType: context.form.entityKind, fromId: findFormFieldById(context.form.fields, context.form.entityPermId, 'identifier', true) as string, selectedEntityType: selectedEntityType });
		} else {
			console.warn("onNewObject callback not provided to context.");
			throw new Error("onNewObject callback not provided to context.");
		}
	};

	static newDatasetAction = (context: IExtendedActionContext, selectedEntityType: string) => {
		if (context.externalAppController) {
			context.externalAppController.createNewObject({ newObjectType: EntityKind.NEW_DATASET, fromObjectType: context.form.entityKind, fromId: findFormFieldById(context.form.fields, context.form.entityPermId, 'identifier', true) as string, selectedEntityType: selectedEntityType });
		} else {
			console.warn("onNewDataset callback not provided to context.");
			throw new Error("onNewDataset callback not provided to context.");
		}
	};
}