import { Form, IExtendedActionContext, } from '@src/js/components/database/new-forms/types/formITypes.ts';
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
  getPropertyFieldsFromAssignments,
  getObjectField,
  getCollectionField,
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { getAutoSaveAction, getCancelAction, getDividerAction, getEditAction, getSaveAction, getDeleteAction } from '@src/js/components/database/new-forms/entities/actionsFieldGetters.ts';

export class DatasetFormModel {

  static adaptDatasetDtoToForm(dto: any): Form {
    const permId = dto.permId.permId;

    const staticFields = [
      getTypeField(dto),
      getPermIdField(dto),
      getCodeField(dto),
      dto.experiment? getCollectionField(dto) : null,
      dto.sample? getObjectField(dto) : null,
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
        // getNewObjectAction(EntityKind.COLLECTION),
				// getNewDatasetAction(EntityKind.COLLECTION),
				// getDividerAction(FormMode.VIEW),
				getEditAction(),
				// getMoveAction(),
				getDeleteAction(),
				//getDividerAction(FormMode.VIEW),
				// getMoreActionsAction(),
				getSaveAction(),
				getCancelAction(),
				getDividerAction(FormMode.EDIT),
				getAutoSaveAction(),
      ]
    };
  }

  static saveDatasetAction = async (context: IExtendedActionContext) => {
    const { form, controller, onAfterSave, mode } = context;
    await new Promise(resolve => setTimeout(resolve, 500));
    const newPermId = await controller.save(form, mode);
    console.log("Dataset saved successfully! New permId:", newPermId);
    if (mode === FormMode.CREATE) {
      alert(`CREATE to be implemented`);
    } else {
      onAfterSave();
    }
  }
}
