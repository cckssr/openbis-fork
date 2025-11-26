import { CoreFormModel } from '@src/js/components/database/new-forms/engine/CoreFormModel.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts';
import { CollectionFormModel } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormModel.ts';
import { ObjectFormModel } from '@src/js/components/database/new-forms/entities/Object/ObjectFormModel.ts';

class ActionHandlerDispatcher {
  static getActionHandler(actionName: string) {
    switch (actionName) {
      // Core actions
      case 'edit':
        return CoreFormModel.editAction;
      case 'cancel':
        return CoreFormModel.cancelEditAction;
      case 'new-form:cancel':
        return CoreFormModel.cancelNewFormAction;
      case 'delete':
        return CoreFormModel.deleteAction;
      case 'move':
        return CoreFormModel.moveAction;
      
      // Space-specific actions
      case 'space:save':
        return SpaceFormModel.saveSpaceAction;
      case 'space:new-project': 
        return SpaceFormModel.newProjectAction;
      case 'space:new-object':
        return SpaceFormModel.newObjectAction;

      // Project-specific actions
      case 'project:save':
        return ProjectFormModel.saveProjectAction;

      // Object-specific actions
      case 'object:save':
        return ObjectFormModel.saveObjectAction;

      // Collection-specific actions
      case 'collection:save':
        return CollectionFormModel.saveCollectionAction;
      
      default:
        console.warn(`Unknown action: ${actionName}`);
        return CoreFormModel.unknownAction(actionName);
    }
  }
}

export default ActionHandlerDispatcher;
