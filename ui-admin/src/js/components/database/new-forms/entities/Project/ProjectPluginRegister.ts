import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { ProjectFormController } from '@src/js/components/database/new-forms/entities/Project/ProjectFormController.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';

export function registerProjectPlugin() {
  FormEngineRegistry.registerController(EntityKind.PROJECT, (openbisFacade) => new ProjectFormController(openbisFacade));
  FormEngineRegistry.registerController(EntityKind.NEW_PROJECT, (openbisFacade) => new ProjectFormController(openbisFacade));

  FormEngineRegistry.registerAction('project:save', ProjectFormModel.saveProjectAction);
}