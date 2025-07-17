import { Form, FormField, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
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
import { editSpaceAction, newProjectAction, saveSpaceAction } from '@src/js/components/database/new-forms/actions/SpaceActions.ts';
import { saveProjectAction } from '@src/js/components/database/new-forms/actions/ProjectActions.ts';
import { cancelEditAction, cancelNewFormAction, editAction } from '@src/js/components/database/new-forms/actions/CoreActions.ts';


export function adaptSpaceDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: 'SPACE',
    title: `Space: ${dto.code}`,
    version: dto.version || 1,
    entityKind: 'SPACE',
    meta: {},
    sections: [
      {
        section: 'Identification Info',
        fields: [ dto.code + '-code', 
          dto.code + '-registrator', 
          dto.code + '-registrationDate', 
          dto.code + '-modifier', 
          dto.code + '-modificationDate' ],
      },
      {
        section: 'General',
        fields: [ dto.code + '-description' ],
      },
    ],
    fields: [
      getCodeField(dto),
      getDescriptionField(dto, { column: 'center' }),
      getRegistratorField(dto),
      getRegistrationDateField(dto),
      getModifierField(dto),
      getModificationDateField(dto),
    ],
    isDirty: false,
    isValid: true,
    actions: [
      {
        name: 'space:save',
        label: 'Save',
        component: 'button',
        handler: saveSpaceAction,
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
        handler: editSpaceAction,
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
        handler: cancelEditAction,
        isAllowed: true,
        visibility: [
          {
            mode: FormMode.EDIT,
          },
        ],
      },
      {
        name: 'space:new-project',
        label: '+ Project',
        component: 'button',
        handler: newProjectAction,
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

export function adaptProjectDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: 'PROJECT',
    title: `Project: ${dto.code}`,
    version: dto.version || 1,
    entityKind: 'PROJECT',
    meta: {},
    sections: [
      {
        section: 'Identification Info',
        fields: [
          dto.code + '-permId',
          dto.code + '-identifier',
          dto.code + '-path',
          dto.code + '-space',
          dto.code + '-code',
          dto.code + '-registrator',
          dto.code + '-registrationDate',
          dto.code + '-modifier',
          dto.code + '-modificationDate',
        ],
      },
      {
        section: 'General',
        fields: [
          dto.code + '-description',
        ],
      },
    ],
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
    isValid: true,
    actions: [
      {
        name: 'save',
        label: 'Save',
        component: 'button',
        handler: saveProjectAction,
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
        handler: editAction,
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
        handler: cancelEditAction,
        isAllowed: true,
        visibility: [
          {
            mode: FormMode.EDIT,
          },
        ],
      },
    ],
  };
}

export function adaptNewProjectDtoToForm(spacePermId: string): Form {
  return {
    entityPermId: spacePermId,
    entityType: 'NEWPROJECT',
    title: `New Project`,
    version: 1,
    entityKind: 'NEWPROJECT',
    meta: {},
    sections: [
      {
        section: 'Identification Info',
        fields: [
          spacePermId + '-code',
        ],
      },
      {
        section: 'General',
        fields: [
          spacePermId + '-description',
        ],
      },
    ],
    fields: [
      getCodeField({}, { readOnly: false, value: '', id: spacePermId + '-code' }),
      getDescriptionField({}, { column: 'center', value: '', id: spacePermId + '-description' }),
    ],
    isDirty: false,
    isValid: true,
    actions: [
      {
        name: 'save',
        label: 'Save',
        component: 'button',
        handler: saveProjectAction,
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
        handler: cancelNewFormAction,
        isAllowed: true,
        visibility: [
          {
            mode: FormMode.CREATE,
          },
        ],
      },
    ],
  } 
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