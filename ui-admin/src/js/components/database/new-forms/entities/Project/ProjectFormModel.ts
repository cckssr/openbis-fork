import { ActionContext, Form, FormMode, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
import { getPermIdField, getIdentifierField, getPathField, getSpaceField, getCodeField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getDescriptionField } from '@src/js/components/database/new-forms/entities/formField.utils.ts';

export class ProjectFormModel {
	
	static adaptProjectDtoToForm(dto: any): Form {
		console.log('adaptProjectDtoToForm', dto);
		const permId = dto.permId.permId;
		return {
			entityPermId: permId,
			entityType: 'PROJECT',
			title: `Project: ${dto.code}`,
			version: dto.version || 1,
			entityKind: 'PROJECT',
			meta: {},
			sections: [
				{
					section: FormSection.IDENTIFICATION_INFO,
					fields: [
						permId + '-permId',
						permId + '-identifier',
						permId + '-path',
						permId + '-space',
						permId + '-code',
						permId + '-registrator',
						permId + '-registrationDate',
						permId + '-modifier',
						permId + '-modificationDate',
					],
				},
				{
					section: FormSection.GENERAL,
					fields: [
						permId + '-description',
					],
				},
			],
			fields: [
				getPermIdField(dto),
				getIdentifierField(dto),
				getPathField(dto),
				getSpaceField(dto),
				getCodeField(dto),
				getRegistratorField(dto),
				getRegistrationDateField(dto),
				getModifierField(dto),
				getModificationDateField(dto),
				getDescriptionField(dto, { column: 'center' }),
			],
			isDirty: false,
			isValid: true,
			actions: [
				{
					name: 'project:save',
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
			],
		};
	}
	
	static adaptNewProjectDtoToForm(spacePermId: string): Form {
		const permId = spacePermId + '-newproject';
		return {
			entityPermId: spacePermId,
			entityType: 'NEWPROJECT',
			title: `New Project`,
			version: 1,
			entityKind: 'NEWPROJECT',
			meta: {},
			sections: [
				{
					section: FormSection.IDENTIFICATION_INFO,
					fields: [
						permId + '-code',
					],
				},
				{
					section: FormSection.GENERAL,
					fields: [
						permId + '-description',
					],
				},
			],
			fields: [
				getCodeField({}, { readOnly: false, value: '', id: permId + '-code' }),
				getDescriptionField({}, { column: 'center', value: '', id: permId + '-description' }),
			],
			isDirty: false,
			isValid: true,
			actions: [
				{
					name: 'project:save',
					label: 'Save',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.CREATE,
						},
					],
				},
				{
					name: 'new-form:cancel',
					label: 'Cancel',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.CREATE,
						},
					],
				},
			],
		}
	}

	static saveProjectAction = async (context: ActionContext) => {
		const { form, openbisFacade, onAfterSave } = context;
		console.log("Saving project:", form);
		// Example of an API call
		// await openbisFacade.updateProject(form.entityPermId, { ... });
		await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate async
		console.log("Project saved successfully!");
		onAfterSave();
	};
}