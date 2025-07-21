import { ActionContext, Form, FormSection } from "@src/js/components/database/new-forms/types/form.types.ts";
import { getCodeField, getPermIdField, getIdentifierField, getPathField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField, getTypeField } from "@src/js/components/database/new-forms/entities/formField.utils.ts";

export class CollectionFormModel {
	
	static adaptCollectionDtoToForm(dto: any): Form {  
	  const permId = dto.permId.permId;
	  return {
		entityPermId: permId,
		entityType: dto.type.code,
		title: `Collection: ${dto.code}`,
		version: dto.version,
		entityKind: 'COLLECTION',
		meta: {},
		sections: [
		  {
			section: FormSection.IDENTIFICATION_INFO,
			fields: [ permId + '-type', 
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
		  getTypeField(dto),
		  getPermIdField(dto),
		  getIdentifierField(dto),
		  getPathField(dto),
		  getCodeField(dto),
		  getRegistratorField(dto),
		  getRegistrationDateField(dto),
		  getModifierField(dto),
		  getModificationDateField(dto),
		],
		isDirty: false,
		isValid: true,
		actions: [
			{
				name: 'collection:save',
				component: 'button',
				label: 'Save',
				visibility: [],
				isAllowed: true,
			},
		],
	  };
	}

	static saveCollectionAction = async (context: ActionContext) => {
		const { form, openbisFacade, onAfterSave } = context;
		console.log("Saving collection:", form);
		await new Promise(resolve => setTimeout(resolve, 1000));
		console.log("Collection saved successfully!");
		onAfterSave();
	}
}