import { Form } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { EntityKind, FormMode, FormSection } from "@src/js/components/database/new-forms/types/formEnums.ts";
import { getCodeField, getPermIdField, getIdentifierField, getPathField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getTypeField, getPropertyFieldsFromAssignments } from "@src/js/components/database/new-forms/entities/formFieldGetters.ts";
import { IExtendedActionContext } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { getNewObjectAction, getNewDatasetAction, getDividerAction, getEditAction, getMoveAction, getAutoSaveAction, getDeleteAction, getCancelAction, getMoreActionsAction, getSaveAction } from "@src/js/components/database/new-forms/entities/actionsFieldGetters.ts";

export class CollectionFormModel {

	static adaptCollectionDtoToForm(dto: any): Form {
		const permId = dto.permId.permId;

		// Get static fields
		const staticFields = [
			getTypeField(dto),
			getPermIdField(dto),
			getIdentifierField(dto),
			getPathField(dto),
			getCodeField(dto),
			getRegistratorField(dto),
			getRegistrationDateField(dto),
			getModifierField(dto),
			getModificationDateField(dto),
		];

		// Get dynamically created fields from propertyAssignments
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
				getSaveAction(EntityKind.COLLECTION),
				getCancelAction(),
				getDividerAction(FormMode.EDIT),
				getAutoSaveAction(),
			],
		};
	}

	static saveCollectionAction = async (context: IExtendedActionContext) => {
		const { form, onAfterSave } = context;
		console.log("Saving collection:", form);
		await new Promise(resolve => setTimeout(resolve, 1000));
		console.log("Collection saved successfully!");
		onAfterSave();
	}
}