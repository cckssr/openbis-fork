import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormMode, FormSection, EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { getPermIdField, getIdentifierField, getPathField, getSpaceField, getCodeField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getDescriptionField } from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';

export class ProjectFormModel {

	static adaptProjectDtoToForm(dto: any): Form {
		const permId = dto.permId.permId;
		const fields = [
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
		];

		return {
			entityPermId: permId,
			entityType: EntityKind.PROJECT,
			title: `Project: ${dto.code}`,
			version: dto.version || 1,
			entityKind: EntityKind.PROJECT,
			meta: {},
			fields,
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
				{
					name: 'delete',
					label: 'Delete',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.VIEW,
						},
					],
				},
				{
					name: 'move',
					label: 'Move',
					component: 'button',
					isAllowed: true,
					visibility: [
						{
							mode: FormMode.VIEW,
						},
					],
				}
			],
		};
	}

	static adaptNewProjectDtoToForm(tmpPermId: string, params: any): Form {
		const permId = tmpPermId + '-' + EntityKind.NEW_PROJECT;
		return {
			entityPermId: tmpPermId,
			entityType: EntityKind.NEW_PROJECT,
			title: `New Project`,
			version: 1,
			entityKind: EntityKind.NEW_PROJECT,
			meta: { spacePermId: params.parentId },
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
				getCodeField({ permId: { permId: permId } }, { readOnly: false, value: '', id: permId + '-code' }),
				getDescriptionField({ permId: { permId: permId } }, { column: 'center', value: '', id: permId + '-description' }),
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
				}
			],
		}
	}

	static saveProjectAction = async (context: IExtendedActionContext) => {
		const { form, controller, onAfterSave, mode } = context;
		await new Promise(resolve => setTimeout(resolve, 500));
		const newPermId = await controller.save(form, mode);
		console.log("Project saved successfully! New permId:", newPermId);
		if (mode === FormMode.CREATE) {
			onAfterSave({ oldType: EntityKind.NEW_PROJECT, oldId: form.entityPermId, newType: EntityKind.PROJECT, newId: newPermId });
		} else {
			onAfterSave();
		}
	};
}