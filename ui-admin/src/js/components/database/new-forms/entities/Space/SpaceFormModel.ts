import { ActionContext, Form } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormSection } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { getCodeField, getDescriptionField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField } from '@src/js/components/database/new-forms/entities/formField.utils.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import objectType from '@src/js/common/consts/objectType.js'

export class SpaceFormModel {

	static adaptSpaceDtoToForm(dto: any): Form {
		const permId = dto.permId.permId;
		return {
			entityPermId: permId,
			entityType: 'SPACE',
			title: `Space: ${dto.code}`,
			version: dto.version || 1,
			entityKind: 'SPACE',
			meta: {},
			sections: [
				{
					section: FormSection.IDENTIFICATION_INFO,
					fields: [permId + '-code',
					permId + '-registrator',
					permId + '-registrationDate',
					permId + '-modifier',
					permId + '-modificationDate'],
				},
				{
					section: FormSection.GENERAL,
					fields: [permId + '-description'],
				},
			],
			fields: [
				getCodeField(dto),
				getDescriptionField(dto, { column: 'center' }),
				getRegistratorField(dto),
				getRegistrationDateField(dto),
				getModifierField(dto),
				getModificationDateField(dto),
			],
			isDirty: false,
			isValid: true,
			actions: [
				{
					name: 'space:save',
					label: 'Save',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.EDIT,
						},
					],
				},
				{
					name: 'edit',
					label: 'Edit',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.VIEW,
						},
					],
				},
				{
					name: 'cancel',
					label: 'Cancel',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.EDIT,
						},
					],
				},
				{
					name: 'space:new-project',
					label: '+ Project',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.VIEW,
						},
					],
				},
				{
					name: 'auto-save',
					label: 'Auto-save',
					component: 'switch',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.EDIT,
						},
					],
				}
			]
		};
	}

	static saveSpaceAction = async (context: ActionContext) => {
		const { form, controller, onAfterSave } = context;
		await new Promise(resolve => setTimeout(resolve, 1000)); // to display the loading spinner
		const newVersion = await controller.save(form);
		console.log("Space saved successfully! New version:", newVersion);
		onAfterSave();
	};

	static newProjectAction = (context: ActionContext) => {
		const { form, externalAppController } = context;
		console.log("SpaceFormModel.newProjectAction", context);
		if (externalAppController) {
			console.log("Invoking onNewObject callback...");
			externalAppController.createNewObject({newObjectType: objectType.NEW_PROJECT, fromObjectType: 'space', fromId: form.entityPermId});
			//onNewObject(objectType.NEW_PROJECT, form.entityPermId);
		} else {
			console.warn("onNewObject callback not provided to context.");
		}
	};
}