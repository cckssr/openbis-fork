import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { getCodeField, getDescriptionField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField } from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import objectType from '@src/js/common/consts/objectType.js'
import { getSaveAction, getEditAction, getCancelAction, getAutoSaveAction, getNewProjectAction, getDividerAction, getDeleteAction, getMoreActionsAction, getNewObjectAction } from '@src/js/components/database/new-forms/entities/actionsFieldGetters.ts';

export class SpaceFormModel {

	static adaptSpaceDtoToForm(dto: any): Form {
		const permId = dto.permId.permId;
		const fields = [
			getCodeField(dto),
			getDescriptionField(dto, { column: 'center' }),
			getRegistratorField(dto),
			getRegistrationDateField(dto),
			getModifierField(dto),
			getModificationDateField(dto),
		];

		return {
			entityPermId: permId,
			entityType: EntityKind.SPACE,
			title: `Space: ${dto.code}`,
			version: dto.version || 1,
			entityKind: EntityKind.SPACE,
			meta: {},
			fields,
			isDirty: false,
			isValid: true,
			actions: [
				getNewProjectAction(),
				getNewObjectAction(EntityKind.SPACE),
				getDividerAction(FormMode.VIEW),
				getEditAction(),
				getDeleteAction(),
				//getDividerAction(FormMode.VIEW),
				//getMoreActionsAction(),
				getSaveAction(EntityKind.SPACE),
				getCancelAction(),
				getDividerAction(FormMode.EDIT),
				getAutoSaveAction(),
			]
		};
	}

	static saveSpaceAction = async (context: IExtendedActionContext) => {
		const { form, mode, controller, onAfterSave } = context;
		await new Promise(resolve => setTimeout(resolve, 500)); // to display the loading spinner
		const newVersion = await controller.save(form, mode);
		onAfterSave();
	};

	static newProjectAction = (context: IExtendedActionContext) => {
		const { form, externalAppController } = context;
		if (externalAppController) {
			externalAppController.createNewObject({newObjectType: objectType.NEW_PROJECT, fromObjectType: EntityKind.SPACE, fromId: form.entityPermId});
		} else {
			console.warn("onNewObject callback not provided to context.");
			throw new Error("onNewObject callback not provided to context.");
		}
	};

	static newObjectAction = (context: IExtendedActionContext) => {
		const { form, externalAppController } = context;
		if (externalAppController) {
			externalAppController.createNewObject({newObjectType: objectType.NEW_OBJECT, fromObjectType: EntityKind.SPACE, fromId: form.entityPermId});
		} else {
			console.warn("onNewObject callback not provided to context.");
			throw new Error("onNewObject callback not provided to context.");
		}
	};
}