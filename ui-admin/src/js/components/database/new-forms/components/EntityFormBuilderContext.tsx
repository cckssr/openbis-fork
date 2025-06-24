import React, { createContext, useContext } from 'react';
import openbis from '@srcV3/openbis.esm';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';

import { SpaceFormController } from '@src/js/components/database/new-forms/controllers/Space/SpaceFormController.ts';
import { ProjectFormController } from '@src/js/components/database/new-forms/controllers/Project/ProjectFormController.ts';


const SpaceFormContext = createContext<SpaceFormController | null>(null);
const ProjectFormContext = createContext<ProjectFormController | null>(null);
//const SampleFormContext = createContext<SampleFormController | null>(null);

interface EntityFormBuilderProviderProps {
  openbisFacade: openbis.openbis;
  children: React.ReactNode;
  entityKind: EntityKind;
  user: string;
}

export const EntityFormBuilderProvider = ({ openbisFacade, children, entityKind, user}: EntityFormBuilderProviderProps) => {
	switch(entityKind){
		case EntityKind.SPACE:
			const spaceController = React.useMemo(() => new SpaceFormController(openbisFacade, user), [openbisFacade, user]);
			return (
			  <SpaceFormContext.Provider value={spaceController}>
				{children}
			  </SpaceFormContext.Provider>
			);
		case EntityKind.PROJECT:
			const projectController = React.useMemo(() => new ProjectFormController(openbisFacade), [openbisFacade]);
			return (
			  <ProjectFormContext.Provider value={projectController}>
				{children}
			  </ProjectFormContext.Provider>
			);
		case EntityKind.SAMPLE:
		default:

	}
  
};

export const useSpaceFormController = () => {
  const ctx = useContext(SpaceFormContext);
  if (!ctx) throw new Error('Must be used within SpaceFormProvider');
  return ctx;
};

export const useProjectFormController = () => {
	const ctx = useContext(ProjectFormContext);
	if (!ctx) throw new Error('Must be used within ProjectFormProvider');
	return ctx;
  };