import { Form } from "@src/js/components/database/new-forms/types/formITypes.ts";
import { FormMode, FormSection } from "@src/js/components/database/new-forms/types/formEnums.ts";
import { getCodeField, getPermIdField, getIdentifierField, getPathField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getTypeField, getPropertyFieldsFromAssignments } from "@src/js/components/database/new-forms/entities/formFieldGetters.ts";
import { IExtendedActionContext } from "@src/js/components/database/new-forms/types/formITypes.ts";

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
			{
				name: 'collection:save',
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

	static saveCollectionAction = async (context: IExtendedActionContext) => {
		const { form, onAfterSave } = context;
		console.log("Saving collection:", form);
		await new Promise(resolve => setTimeout(resolve, 1000));
		console.log("Collection saved successfully!");
		onAfterSave();
	}
}