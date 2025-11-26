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


export function adaptDatasetDtoToForm(dto: any): Form {
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
    isValid: true
  };
}