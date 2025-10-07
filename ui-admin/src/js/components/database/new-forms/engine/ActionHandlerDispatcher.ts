import { CoreFormModel } from '@src/js/components/database/new-forms/engine/CoreFormModel.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts';
import { CollectionFormModel } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormModel.ts';

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
      
      // Space-specific actions
      case 'space:save':
        return SpaceFormModel.saveSpaceAction;
      case 'space:new-project': 
        return SpaceFormModel.newProjectAction

      // Project-specific actions
      case 'project:save':
        return ProjectFormModel.saveProjectAction;

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
