import { ProjectFormController } from '@src/js/components/database/new-forms/entities/Project/ProjectFormController.ts';
import { SpaceFormController } from '@src/js/components/database/new-forms/entities/Space/SpaceFormController.ts';
import { CollectionFormController } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormController.ts';
import { DatasetFormController } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormController.ts';
import { ObjectFormController } from '@src/js/components/database/new-forms/entities/Object/ObjectFormController.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';

class ControllerDispatcher {
  static createController(entityKind: string, openbisFacade: any, user?: string) {
    switch (entityKind) {
      case EntityKind.SPACE:
      case EntityKind.NEW_SPACE:
        return new SpaceFormController(openbisFacade);
      case EntityKind.PROJECT:
      case EntityKind.NEW_PROJECT:
        return new ProjectFormController(openbisFacade);
      case EntityKind.COLLECTION:
      case EntityKind.NEW_COLLECTION:
        return new CollectionFormController(openbisFacade);
      case EntityKind.DATASET:
      case EntityKind.NEW_DATASET:
        return new DatasetFormController(openbisFacade);
      case EntityKind.OBJECT:
      case EntityKind.NEW_OBJECT:
        return new ObjectFormController(openbisFacade);
      default:
        throw new Error(`Unknown entity kind: ${entityKind}`);
    }
  }
}

export default ControllerDispatcher;
