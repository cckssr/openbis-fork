import { Form, FormField, IExtendedActionContext, } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { EntityKind, FormMode, FormSection } from '@src/js/components/database/new-forms/types/formEnums.ts';
import {
  getCodeField,
  getProjectField,
  getPermIdField,
  getIdentifierField,
  getPathField,
  getSpaceField,
  getRegistratorField,
  getRegistrationDateField,
  getModifierField,
  getModificationDateField,
  getTypeField,
  getPropertyFieldsFromAssignments
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';

export class ObjectFormModel {

  static adaptSampleDtoToForm(dto: any): Form {
    const permId = dto.permId.permId;

    const staticFields = [
      getTypeField(dto),
      getPermIdField(dto),
      getIdentifierField(dto),
      getPathField(dto),
      getSpaceField(dto),
      getProjectField(dto),
      getCodeField(dto),
      getRegistratorField(dto),
      getRegistrationDateField(dto),
      getModifierField(dto),
      getModificationDateField(dto),
    ];

    const propertyFields = getPropertyFieldsFromAssignments(dto);
    const fields = [...staticFields, ...propertyFields];

    return {
      entityPermId: permId,
      entityType: dto.type.code,
      title: `Sample: ${dto.code}`,
      version: dto.version,
      entityKind: EntityKind.SAMPLE,
      meta: {},
      fields,
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