import React, { createContext, useContext } from 'react';
import openbis from '@srcV3/openbis.esm';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';

import { SpaceFormController } from '@src/js/components/database/new-forms/controllers/Space/SpaceFormController.ts';
import { ProjectFormController } from '@src/js/components/database/new-forms/controllers/Project/ProjectFormController.ts';
import { SampleFormController } from '@src/js/components/database/new-forms/controllers/Sample/SampleFormController.ts';

const SpaceFormContext = createContext<any | null>(null);
const ProjectFormContext = createContext<any | null>(null);
const SampleFormContext = createContext<any | null>(null);

interface EntityFormBuilderProviderProps {
	openbisFacade: openbis.openbis;
	children: React.ReactNode;
	entityKind: EntityKind;
	user: string;
	onEntityChange: (permId: string, changed: boolean) => void;
	onNewProject: () => void;
}

export const EntityFormBuilderProvider = ({ openbisFacade, children, entityKind, user, onEntityChange, onNewProject }: EntityFormBuilderProviderProps) => {
	switch (entityKind) {
		case EntityKind.SPACE:
			const spaceController = React.useMemo(() => new SpaceFormController(openbisFacade, user), [openbisFacade, user]);
			return (
				<SpaceFormContext.Provider value={{ spaceController, onEntityChange, onNewProject }}>
					{children}
				</SpaceFormContext.Provider>
			);
		case EntityKind.PROJECT:
			const projectController = React.useMemo(() => new ProjectFormController(openbisFacade), [openbisFacade]);
			return (
				<ProjectFormContext.Provider value={{ projectController, onEntityChange }}>
					{children}
				</ProjectFormContext.Provider>
			);
		case EntityKind.SAMPLE:
			const sampleController = React.useMemo(() => new SampleFormController(openbisFacade), [openbisFacade]);
			return (
				<SampleFormContext.Provider value={{ sampleController, onEntityChange }}>
					{children}
				</SampleFormContext.Provider>
			);
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

export const useSampleFormController = () => {
	const ctx = useContext(SampleFormContext);
	if (!ctx) throw new Error('Must be used within SampleFormProvider');
	return ctx;
};