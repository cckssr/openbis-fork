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
  getPropertyFieldsFromAssignments,
  getCollectionField,
  getObjectField
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import {
  getMoveAction, getDeleteAction, getEditAction, getDividerAction, getMoreActionsAction,
  getSaveAction, getCancelAction, getNewObjectAction, getAutoSaveAction, getNewDatasetAction
} from '@src/js/components/database/new-forms/entities/actionsFieldGetters.ts';

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
      getCollectionField(dto),
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
        getNewObjectAction(EntityKind.OBJECT),
        getNewDatasetAction(EntityKind.OBJECT),
        getDividerAction(FormMode.VIEW),
        getEditAction(),
        getMoveAction(),
        getDeleteAction(),
        getDividerAction(FormMode.VIEW),
        getMoreActionsAction(),
        getSaveAction(EntityKind.OBJECT),
        getCancelAction(),
        getDividerAction(FormMode.EDIT),
        getAutoSaveAction(),
      ]
    };
  }

  static adaptNewObjectDtoToForm(dto: any, tmpPermId: string, params: any): Form {
    console.log('ObjectFormModel.adaptNewObjectDtoToForm: dto', { dto, tmpPermId, params });
    const permId = tmpPermId + '-' + EntityKind.NEW_OBJECT;
    const parentType = params.parentType;
    const parentTypeField = parentType === EntityKind.SPACE ? getSpaceField({ permId: { permId: permId } }, { value: params.parentIdentifier, id: permId + '-space' }) :
      parentType === EntityKind.PROJECT ? getProjectField({ permId: { permId: permId } }, { value: params.parentIdentifier, id: permId + '-project' }) :
      (parentType === EntityKind.COLLECTION || parentType === EntityKind.EXPERIMENT) ? getCollectionField({ permId: { permId: permId } }, { value: params.parentIdentifier, id: permId + '-collection' }) :
      (parentType === EntityKind.OBJECT || parentType === EntityKind.SAMPLE) ? getObjectField({ permId: { permId: permId } }, { value: params.parentIdentifier, id: permId + '-object' }) : null;
    if (!parentTypeField) {
      throw new Error(`Parent type ${parentType} not supported`);
    }
    const staticFields = [
      getCodeField({ permId: { permId: permId } }, { readOnly: false, value: '', id: permId + '-code' }),
			parentTypeField,
      getTypeField({ permId: { permId: permId } }, { value: params.entityType, id: permId + '-entityType' }),
    ];

    const propertyFields = getPropertyFieldsFromAssignments(dto);
    const fields = [...staticFields, ...propertyFields];

    return {
      entityPermId: permId,
      entityType: params.entityType,
      title: `New Object`,
      version: 1,
      entityKind: EntityKind.NEW_OBJECT,
      meta: {},
      fields: fields.filter(field => field !== null),
      isDirty: false,
      isValid: true,
      actions: [
        getSaveAction(),
        getCancelAction(true),
      ],
    }
  }

  static saveObjectAction = async (context: IExtendedActionContext) => {
    const { form, controller, onAfterSave, mode } = context;
    await new Promise(resolve => setTimeout(resolve, 500));
    const newPermId = await controller.save(form, mode);
    if (mode === FormMode.CREATE) {
      onAfterSave({ oldType: EntityKind.NEW_OBJECT, oldId: form.entityPermId, newType: EntityKind.OBJECT, newId: newPermId });
    } else {
      onAfterSave();
    }
  };
}