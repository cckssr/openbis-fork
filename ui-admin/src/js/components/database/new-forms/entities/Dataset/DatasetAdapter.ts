import { Form, } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormSection } from '@src/js/components/database/new-forms/types/form.enums.ts';
import {
  getCodeField,
  getDescriptionField,
  getPermIdField,
  getIdentifierField,
  getPathField,
  getRegistratorField,
  getRegistrationDateField,
  getModifierField,
  getModificationDateField,
  getTypeField
} from '@src/js/components/database/new-forms/entities/formField.utils.ts';


export function adaptDatasetDtoToForm(dto: any): Form {
  const permId = dto.permId.permId;
  return {
    entityPermId: permId,
    entityType: dto.type.code,
    title: `Dataset: ${dto.code}`,
    version: dto.version,
    entityKind: 'DATASET',
    meta: {},
    sections: [
      {
        section: FormSection.IDENTIFICATION_INFO,
        fields: [ permId + '-type', 
          permId + '-permId', 
          permId + '-identifier', 
          permId + '-path',
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
      getCodeField(dto),
      getRegistratorField(dto),
      getRegistrationDateField(dto),
      getModifierField(dto),
      getModificationDateField(dto),
    ],
    isDirty: false,
    isValid: true
  };
}