import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/form.types.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { getCodeField, getDescriptionField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField } from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import objectType from '@src/js/common/consts/objectType.js'

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
				
			]
		};
	}

/* 	static saveSpaceAction = async (context: IExtendedActionContext) => {
		const { form, mode, controller, onAfterSave } = context;
		await new Promise(resolve => setTimeout(resolve, 500)); // to display the loading spinner
		const newVersion = await controller.save(form, mode);
		console.log("Space saved successfully! New version:", newVersion);
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
	}; */
}