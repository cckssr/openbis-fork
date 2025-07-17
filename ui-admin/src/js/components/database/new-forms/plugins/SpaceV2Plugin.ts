import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { SpaceFormController } from '@src/js/components/database/new-forms/entities/Space/SpaceFormController.ts';
//import SpaceFormViewV2 from '@src/js/components/database/new-forms/entities/Space/SpaceFormViewV2.tsx';
import { newProjectAction, saveSpaceAction } from '@src/js/components/database/new-forms/actions/SpaceActions.ts';

export function registerSpaceV2Plugin() {
	console.log('registerSpaceV2Plugin');
	FormEngineRegistry.registerController('SPACE', (openbisFacade, user) => new SpaceFormController(openbisFacade, user));
	//FormEngineRegistry.registerFormView('SPACE', SpaceFormViewV2);
	FormEngineRegistry.registerAction('space:save', saveSpaceAction);
	FormEngineRegistry.registerAction('space:new-project', newProjectAction);
} 