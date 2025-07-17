import React, { createContext, useContext, useMemo } from 'react';
import openbis from '@srcV3/openbis.esm';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';

import { SpaceFormController } from '@src/js/components/database/new-forms/entities/Space/SpaceFormController';
import { ProjectFormController } from '@src/js/components/database/new-forms/entities/Project/ProjectFormController';
import { ObjectFormController } from '@src/js/components/database/new-forms/entities/Object/ObjectFormController';
import { CollectionFormController } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormController';
import { DatasetFormController } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormController';

// Single context for all entity forms
const EntityFormContext = createContext<any | null>(null);

interface EntityFormProviderProps {
	openbisFacade: openbis.openbis;
	children: React.ReactNode;
	entityKind: EntityKind;
	user: string;
	onEntityChange: (permId: string, changed: boolean) => void;
	onNewProject: () => void;
}

export const EntityFormProvider = ({ 
	openbisFacade, 
	children, 
	entityKind, 
	user, 
	onEntityChange, 
	onNewProject 
}: EntityFormProviderProps) => {
	
	const controller = useMemo(() => {
		switch (entityKind) {
			case EntityKind.SPACE:
				return new SpaceFormController(openbisFacade, user);
			case EntityKind.PROJECT:
			case EntityKind.NEW_PROJECT:
				return new ProjectFormController(openbisFacade);
			case EntityKind.SAMPLE:
				return new ObjectFormController(openbisFacade);
			case EntityKind.COLLECTION:
				return new CollectionFormController(openbisFacade);
			case EntityKind.DATASET:
				return new DatasetFormController(openbisFacade);
			default:
				throw new Error(`Unsupported entity kind: ${entityKind}`);
		}
	}, [openbisFacade, user, entityKind]);

	const contextValue = useMemo(() => ({
		controller,
		entityKind,
		onEntityChange,
		onNewProject
	}), [controller, entityKind, onEntityChange, onNewProject]);

	return (
		<EntityFormContext.Provider value={contextValue}>
			{children}
		</EntityFormContext.Provider>
	);
};

export const useEntityForm = () => {
	const ctx = useContext(EntityFormContext);
	if (!ctx) {
		throw new Error('useEntityForm must be used within EntityFormProvider');
	}
	return ctx;
};

// Type-safe hooks for specific entity types
export const useSpaceForm = () => {
	const ctx = useEntityForm();
	if (ctx.entityKind !== EntityKind.SPACE) {
		throw new Error('useSpaceForm must be used with SPACE entity kind');
	}
	return ctx;
};

export const useProjectForm = () => {
	const ctx = useEntityForm();
	if (ctx.entityKind !== EntityKind.PROJECT && ctx.entityKind !== EntityKind.NEW_PROJECT) {
		throw new Error('useProjectForm must be used with PROJECT entity kind');
	}
	return ctx;
};

export const useObjectForm = () => {
	const ctx = useEntityForm();
	if (ctx.entityKind !== EntityKind.SAMPLE) {
		throw new Error('useObjectForm must be used with SAMPLE entity kind');
	}
	return ctx;
}; 

export const useCollectionForm = () => {
	const ctx = useEntityForm();
	if (ctx.entityKind !== EntityKind.COLLECTION && ctx.entityKind !== EntityKind.DATASET) {
		throw new Error('useCollectionForm must be used with COLLECTION or DATASET entity kind');
	}
	return ctx;
};

export const useDatasetForm = () => {
	const ctx = useEntityForm();
	if (ctx.entityKind !== EntityKind.DATASET) {
		throw new Error('useDatasetForm must be used with DATASET entity kind');
	}
	return ctx;
};