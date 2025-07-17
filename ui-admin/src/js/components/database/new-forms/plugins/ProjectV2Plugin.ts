import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { ProjectFormController } from '@src/js/components/database/new-forms/entities/Project/ProjectFormController.ts';
//import ProjectFormViewV2 from '@src/js/components/database/new-forms/entities/Project/ProjectFormViewV2.tsx';
import { saveProjectAction } from '@src/js/components/database/new-forms/actions/ProjectActions.ts';

/* export function registerProjectV2Plugin(openbisFacade: any, user: string) {
  FormEngineRegistry.registerController('PROJECT', (openbisFacade, user) => new ProjectFormController(openbisFacade, user));
  FormEngineRegistry.registerFormView('PROJECT', ProjectFormViewV2);
}  */

// Assume ProjectFormLoader handles loading the schema now
// import { ProjectFormLoader } from './loaders/ProjectFormLoader';

export function registerProjectV2Plugin() {
  // The controller is replaced by a simpler loader/service
  FormEngineRegistry.registerController('PROJECT', (openbisFacade, user) => new ProjectFormController(openbisFacade, user));
  FormEngineRegistry.registerController('NEWPROJECT', (openbisFacade, user) => new ProjectFormController(openbisFacade, user));
  //FormEngineRegistry.registerFormView('PROJECT', ProjectFormViewV2);

  // Register the specific actions for this entity type
  FormEngineRegistry.registerAction('project:save', saveProjectAction);

}