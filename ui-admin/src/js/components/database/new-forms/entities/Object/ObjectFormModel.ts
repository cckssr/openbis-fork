import { Form, FormField, IExtendedActionContext, } from '@src/js/components/database/new-forms/types/form.types.ts';
import { EntityKind, FormMode, FormSection } from '@src/js/components/database/new-forms/types/form.enums.ts';
import {
  getCodeField,
  getDescriptionField,
  getPermIdField,
  getIdentifierField,
  getPathField,
  getSpaceField,
  getRegistratorField,
  getRegistrationDateField,
  getModifierField,
  getModificationDateField,
  getTypeField,
  getObjectTypeCodeField,
  getShowOnProjectOverviewField,
  getDocumentField
} from '@src/js/components/database/new-forms/entities/formField.utils.ts';

export class ObjectFormModel {

  static adaptSampleDtoToForm(dto: any): Form {
    const permId = dto.permId.permId;
    return {
      entityPermId: permId,
      entityType: dto.type.code,
      title: `Sample: ${dto.code}`,
      version: dto.version,
      entityKind: EntityKind.SAMPLE,
      meta: {},
      sections: [
        {
          section: FormSection.GENERAL,
          fields: [
            permId + '-document',
          ],
        },
        {
          section: FormSection.IDENTIFICATION_INFO,
          fields: [
            permId + '-entityType',
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
      ],
      fields: [
        getTypeField(dto),
        getPermIdField(dto),
        getIdentifierField(dto),
        getPathField(dto),
        getSpaceField(dto),
        getCodeField(dto),
        getRegistratorField(dto),
        getRegistrationDateField(dto),
        getModifierField(dto),
        getModificationDateField(dto),
        getDocumentField(dto),
      ],
      isDirty: false,
      isValid: true,
      actions: [
        {
					name: 'object:save',
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
      ]
    };
  }

  static adaptNewDefaultObjectDtoToForm(type: string, tmpPermId: string, params: any): Form {
    console.log('ObjectFormModel.adaptNewObjectDtoToForm', { type, tmpPermId, params });
    const permId = tmpPermId + '-' + EntityKind.NEW_OBJECT;
    return {
      entityPermId: tmpPermId,
      entityType: type,
      title: `New Object`,
      version: 1,
      entityKind: EntityKind.NEW_OBJECT,
      meta: { spacePermId: params.parentId },
      sections: [
        {
          section: FormSection.SELECT_TYPE,
          fields: [
            permId + '-objectTypeCode',
          ],
        },
      ],
      fields: [
        getObjectTypeCodeField({ permId: { permId: permId } }, { readOnly: false, value: '', id: permId + '-objectTypeCode' }),
      ],
      isDirty: false,
      isValid: true,
      actions: [
        {
          name: 'object:save',
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

  static adaptNewEntryDtoToForm(type: string, tmpPermId: string, params: any): Form {
    console.log('ObjectFormModel.adaptNewEntryDtoToForm', { type, tmpPermId, params });
    const permId = tmpPermId + '-' + EntityKind.NEW_OBJECT;
    return {
      entityPermId: tmpPermId,
      entityType: type,
      title: `New ENTRY`,
      version: 1,
      entityKind: EntityKind.NEW_OBJECT,
      meta: { spacePermId: params.parentId },
      sections: [
        {
          section: FormSection.IDENTIFICATION_INFO,
          fields: [
            permId + '-objectTypeCode',
            permId + '-code',
          ],
        },
        {
          section: FormSection.GENERAL,
          fields: [
            permId + '-showOnProjectOverview',
            permId + '-document',
          ],
        },
      ],
      fields: [
        getCodeField({ permId: { permId: permId } }, { readOnly: false, value: params.defaultCode, id: permId + '-code' }),
        getTypeField({ permId: { permId: permId } }, { value: 'ENTRY', id: permId + '-objectTypeCode' }),
        getShowOnProjectOverviewField({ permId: { permId: permId } }, { value: true, id: permId + '-showOnProjectOverview' }),
        getDocumentField({ permId: { permId: permId } }, { column: 'center', value: '', id: permId + '-document' }),
      ],
      isDirty: false,
      isValid: true,
      actions: [
        {
          name: 'object:save',
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

  static saveObjectAction = async (context: IExtendedActionContext) => {
    const { form, controller, onAfterSave, mode } = context;
    await new Promise(resolve => setTimeout(resolve, 500));
    const newPermId = await controller.save(form, mode);
    console.log("Object saved successfully! New permId:", newPermId);
    if (mode === FormMode.CREATE) {
      onAfterSave({ oldType: EntityKind.NEW_OBJECT, oldId: form.entityPermId, newType: EntityKind.OBJECT, newId: newPermId });
    } else {
      onAfterSave();
    }
  };
}