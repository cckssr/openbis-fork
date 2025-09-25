import { EntityKind } from '@src/js/components/database/new-forms/types/form.enums.ts';
import objectType from '@src/js/common/consts/objectType.js'
import { ProjectFormController } from '@src/js/components/database/new-forms/entities/Project/ProjectFormController.ts';
import { SpaceFormController } from '@src/js/components/database/new-forms/entities/Space/SpaceFormController.ts';
import { CollectionFormController } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormController.ts';
import { DatasetFormController } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormController.ts';
import { ObjectFormController } from '@src/js/components/database/new-forms/entities/Object/ObjectFormController.ts';

class ControllerDispatcher {
  static createController(entityKind: string, openbisFacade: any, user?: string) {
    switch (entityKind) {
      case objectType.SPACE:
        return new SpaceFormController(openbisFacade);
      case objectType.PROJECT:
      case objectType.NEW_PROJECT:
        return new ProjectFormController(openbisFacade);
      case objectType.COLLECTION:
        return new CollectionFormController(openbisFacade);
      case objectType.DATA_SET:
        return new DatasetFormController(openbisFacade);
      case objectType.OBJECT:
        return new ObjectFormController(openbisFacade);
      default:
        throw new Error(`Unknown entity kind: ${entityKind}`);
    }
  }
}

export default ControllerDispatcher;
