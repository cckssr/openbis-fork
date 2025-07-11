import { Form, FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
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
} from '@src/js/components/database/new-forms/adapters/formField.utils.ts';

export function adaptSpaceDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: 'SPACE',
    title: `Space: ${dto.code}`,
    version: dto.version || 1,
    entityKind: 'SPACE',
    meta: {},
    fields: [
      getCodeField(dto, { readOnly: false, required: true }),
      getDescriptionField(dto, { column: 'center' }),
      getRegistratorField(dto),
      getRegistrationDateField(dto),
      getModifierField(dto),
      getModificationDateField(dto),
    ],
    isDirty: false,
    isValid: true
  };
}

export function adaptProjectDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: 'PROJECT',
    title: `Project: ${dto.code}`,
    version: dto.version || 1,
    entityKind: 'PROJECT',
    meta: {},
    fields: [
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
    ],
    isDirty: false,
    isValid: true
  };
}

export function adaptSampleDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: dto.type.code,
    title: `Sample: ${dto.code}`,
    version: dto.version,
    entityKind: 'SAMPLE',
    meta: {},
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

export function adaptCollectionDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: dto.type.code,
    title: `Collection: ${dto.code}`,
    version: dto.version,
    entityKind: 'COLLECTION',
    meta: {},
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
    isValid: true
  };
}

export function adaptDatasetDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: dto.type.code,
    title: `Dataset: ${dto.code}`,
    version: dto.version,
    entityKind: 'DATASET',
    meta: {}, 
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