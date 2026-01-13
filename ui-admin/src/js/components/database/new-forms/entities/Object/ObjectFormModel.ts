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
  getCollectionField
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
    const permId = tmpPermId + '-' + EntityKind.NEW_OBJECT;

    const staticFields = [
      getCodeField({ permId: { permId: permId } }, { readOnly: false, value: '', id: permId + '-code' }),
			getProjectField({ permId: { permId: permId } }, { value: params.parentId, id: permId + '-project' }),
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
      fields,
      isDirty: false,
      isValid: true,
      actions: [
        getSaveAction(),
        getCancelAction(),
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