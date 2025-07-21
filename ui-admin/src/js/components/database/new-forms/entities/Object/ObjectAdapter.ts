import { Form, FormField, FormMode, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
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
  getTypeField
} from '@src/js/components/database/new-forms/entities/formField.utils';

export function adaptSampleDtoToForm(dto: any): Form {
  const permId = dto.permId.permId;
  return {
    entityPermId: permId,
    entityType: dto.type.code,
    title: `Sample: ${dto.code}`,
    version: dto.version,
    entityKind: 'SAMPLE',
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
      getSpaceField(dto),
      getCodeField(dto),
      getRegistratorField(dto),
      getRegistrationDateField(dto),
      getModifierField(dto),
      getModificationDateField(dto),
      getDescriptionField(dto),
    ],
    isDirty: false,
    isValid: true
  };
}
