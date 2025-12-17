import { Form } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { EntityKind, FormMode, FormSection } from "@src/js/components/database/new-forms/types/formEnums.ts";
import { getCodeField, getPermIdField, getIdentifierField, getPathField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getTypeField, getPropertyFieldsFromAssignments, getProjectField } from "@src/js/components/database/new-forms/entities/formFieldGetters.ts";
import { IExtendedActionContext } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { getNewObjectAction, getNewDatasetAction, getDividerAction, getEditAction, getMoveAction, getAutoSaveAction, getDeleteAction, getCancelAction, getMoreActionsAction, getSaveAction } from "@src/js/components/database/new-forms/entities/actionsFieldGetters.ts";

export class CollectionFormModel {

	static adaptCollectionDtoToForm(dto: any): Form {
		const permId = dto.permId.permId;

		const staticFields = [
			getTypeField(dto),
			getPermIdField(dto),
			getCodeField(dto),
			getProjectField(dto),
			getIdentifierField(dto),
			getPathField(dto),
			getRegistratorField(dto),
			getRegistrationDateField(dto),
			getModifierField(dto),
			getModificationDateField(dto),
		];

		const propertyFields = getPropertyFieldsFromAssignments(dto);

		return {
			entityPermId: permId,
			entityType: dto.type.code,
			title: `Collection: ${dto.code}`,
			version: dto.version,
			entityKind: 'COLLECTION',
			meta: {},
			fields: [...staticFields, ...propertyFields],
			isDirty: false,
			isValid: true,
			actions: [
				// getNewObjectAction(EntityKind.COLLECTION),
				// getNewDatasetAction(EntityKind.COLLECTION),
				// getDividerAction(FormMode.VIEW),
				getEditAction(),
				// getMoveAction(),
				// getDeleteAction(),
				//getDividerAction(FormMode.VIEW),
				// getMoreActionsAction(),
				getSaveAction(),
				getCancelAction(),
				getDividerAction(FormMode.EDIT),
				getAutoSaveAction(),
			],
		};
	}

	static saveCollectionAction = async (context: IExtendedActionContext) => {
		const { form, controller, onAfterSave, mode } = context;
		await new Promise(resolve => setTimeout(resolve, 500));
		const newPermId = await controller.save(form, mode);
		console.log("Collection saved successfully! New permId:", newPermId);
		if (mode === FormMode.CREATE) {
			alert(`CREATE to be implemented`);
		} else {
			onAfterSave();
		}
	}
}