import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormMode, FormSection, EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { getPermIdField, getIdentifierField, getPathField, getSpaceField, getCodeField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getDescriptionField } from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import { getAutoSaveAction, getCancelAction, getDividerAction, getEditAction, getMoveAction, getSaveAction, getNewCollectionAction, getDeleteAction, getNewObjectAction, getMoreActionsAction } from '@src/js/components/database/new-forms/entities/actionsFieldGetters.ts';
import objectType from '@src/js/common/consts/objectType';

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
				getNewCollectionAction(EntityKind.PROJECT),
				getNewObjectAction(EntityKind.PROJECT),
				getDividerAction(FormMode.VIEW),
				getEditAction(),
				getMoveAction(),
				getDeleteAction(),
				getDividerAction(FormMode.VIEW),
				getMoreActionsAction(),
				getSaveAction(EntityKind.PROJECT),
				getCancelAction(),
				getDividerAction(FormMode.EDIT),
				getAutoSaveAction(),
				
			],
		};
	}

	static adaptNewProjectDtoToForm(tmpPermId: string, params: any): Form {
		const permId = tmpPermId + '-' + EntityKind.NEW_PROJECT;
		return {
			entityPermId: permId,
			entityType: EntityKind.PROJECT,
			title: `New Project`,
			version: 1,
			entityKind: EntityKind.NEW_PROJECT,
			meta: { spacePermId: params.parentId },
			fields: [
				getCodeField({ permId: { permId: permId } }, { readOnly: false, value: '', id: permId + '-code' }),
				getSpaceField({ permId: { permId: permId } }, { value: params.parentId, id: permId + '-space' }),
				getDescriptionField({ permId: { permId: permId } }, { column: 'center', value: '', id: permId + '-description' }),
			],
			isDirty: false,
			isValid: true,
			actions: [
				getSaveAction(),
				getCancelAction(),
			],
		}
	}

	static saveProjectAction = async (context: IExtendedActionContext) => {
		const { form, controller, onAfterSave, mode } = context;
		await new Promise(resolve => setTimeout(resolve, 500));
		const newPermId = await controller.save(form, mode);
		if (mode === FormMode.CREATE) {
			onAfterSave({ oldType: EntityKind.NEW_PROJECT, oldId: form.entityPermId, newType: EntityKind.PROJECT, newId: newPermId });
		} else {
			onAfterSave();
		}
	};
}