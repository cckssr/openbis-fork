import { Form, IExtendedActionContext, } from '@src/js/components/database/new-forms/types/formITypes.ts';
import {
  getCodeField,
  getPermIdField,
  getPathField,
  getRegistratorField,
  getRegistrationDateField,
  getModifierField,
  getModificationDateField,
  getTypeField,
  getPropertyFieldsFromAssignments,
  getObjectField,
  getCollectionField
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { getAutoSaveAction, getCancelAction, getDividerAction, getEditAction, getSaveAction, getDeleteAction, getMoveAction, getNewObjectAction, getNewDatasetAction, getMoreActionsAction } from '@src/js/components/database/new-forms/entities/actionsFieldGetters.ts';

export class DatasetFormModel {

  static adaptDatasetDtoToForm(dto: any): Form {
    const permId = dto.permId.permId;

    const staticFields = [
      getTypeField(dto),
      getPermIdField(dto),
      getCodeField(dto),
      dto.experiment ? getCollectionField(dto) : null,
      dto.sample ? getObjectField(dto) : null,
      getPathField(dto),
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
      entityKind: EntityKind.DATASET,
      meta: {},
      fields: [...staticFields.filter(field => field !== null), ...propertyFields],
      isDirty: false,
      isValid: true,
      actions: [
        getNewObjectAction(EntityKind.DATASET),
        getNewDatasetAction(EntityKind.DATASET),
        getDividerAction(FormMode.VIEW),
        getEditAction(),
        getMoveAction(),
        getDeleteAction(),
        getDividerAction(FormMode.VIEW),
        getMoreActionsAction(),
        getSaveAction(),
        getCancelAction(),
        getDividerAction(FormMode.EDIT),
        getAutoSaveAction(),
      ]
    };
  }

  static adaptNewDatasetDtoToForm(dto: any, tmpPermId: string, params: any): Form {
    const permId = tmpPermId + '-' + EntityKind.NEW_DATASET;
    const parentType = params.parentType;
    const parentTypeField = (parentType === EntityKind.COLLECTION || parentType === EntityKind.EXPERIMENT) ? getCollectionField({ permId: { permId: permId } }, { value: params.parentId, id: permId + '-collection' }) :
      (parentType === EntityKind.OBJECT || parentType === EntityKind.SAMPLE) ? getObjectField({ permId: { permId: permId } }, { value: params.parentId, id: permId + '-object' }) : null;
    if (!parentTypeField) {
      throw new Error(`Parent type ${parentType} not supported`);
    }
    const staticFields = [
      getCodeField({ permId: { permId: permId } }, { readOnly: false, value: '', id: permId + '-code' }),
      getTypeField({ permId: { permId: permId } }, { value: params.entityType, id: permId + '-entityType' }),
      parentTypeField,
    ];
    const propertyFields = getPropertyFieldsFromAssignments(dto);

    return {
      entityPermId: permId,
      entityType: params.entityType,
      title: `New Dataset`,
      version: 1,
      entityKind: EntityKind.NEW_DATASET,
      meta: {},
      fields: [
        ...staticFields,
        ...propertyFields,
      ],
      isDirty: false,
      isValid: true,
      actions: [
        getSaveAction(),
        getCancelAction(true),
      ],
    }
  }

  static saveDatasetAction = async (context: IExtendedActionContext) => {
    const { form, controller, onAfterSave, mode } = context;
    await new Promise(resolve => setTimeout(resolve, 500));
    const newPermId = await controller.save(form, mode);
    if (mode === FormMode.CREATE) {
      alert(`CREATE to be implemented`);
      //onAfterSave({ oldType: EntityKind.NEW_DATASET, oldId: form.entityPermId, newType: EntityKind.DATASET, newId: newPermId });
    } else {
      onAfterSave();
    }
  }
}
