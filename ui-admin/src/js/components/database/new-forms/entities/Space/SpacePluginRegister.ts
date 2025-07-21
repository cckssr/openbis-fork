import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { SpaceFormController } from '@src/js/components/database/new-forms/entities/Space/SpaceFormController.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';

export function registerSpacePlugin() {
	FormEngineRegistry.registerController(EntityKind.SPACE, (openbisFacade) => new SpaceFormController(openbisFacade));
	
	FormEngineRegistry.registerAction('space:save', SpaceFormModel.saveSpaceAction);
	FormEngineRegistry.registerAction('space:new-project', SpaceFormModel.newProjectAction);
} 