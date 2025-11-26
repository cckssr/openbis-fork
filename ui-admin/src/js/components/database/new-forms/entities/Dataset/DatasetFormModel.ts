import { Form, } from '@src/js/components/database/new-forms/types/formITypes.ts';
import {
  getCodeField,
  getPermIdField,
  getIdentifierField,
  getPathField,
  getRegistratorField,
  getRegistrationDateField,
  getModifierField,
  getModificationDateField,
  getTypeField,
  getPropertyFieldsFromAssignments
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

export class DatasetFormModel {
  static adaptDatasetDtoToForm(dto: any): Form {
    const permId = dto.permId.permId;
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
    const propertyFields = getPropertyFieldsFromAssignments(dto);
    return {
      entityPermId: permId,
      entityType: dto.type.code,
      title: `Dataset: ${dto.code}`,
      version: dto.version,
      entityKind: 'DATASET',
      meta: {},
      fields: [...staticFields, ...propertyFields],
      isDirty: false,
      isValid: true,
      actions: [
        {
          name: 'dataset:save',
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
}
