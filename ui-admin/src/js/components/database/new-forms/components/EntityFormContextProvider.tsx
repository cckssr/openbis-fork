import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

import { EntityKind, Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

import { SpaceFormController } from '@src/js/components/database/new-forms/entities/Space/SpaceFormController.ts';
import { ProjectFormController } from '@src/js/components/database/new-forms/entities/Project/ProjectFormController.ts';
import { ObjectFormController } from '@src/js/components/database/new-forms/entities/Object/ObjectFormController.ts';
import { CollectionFormController } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormController.ts';
import { DatasetFormController } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormController.ts';
import { objectTypeToEntityKindMap } from '@src/js/components/database/new-forms/utils/Utils.ts';
import EntityFormContainer from '@src/js/components/database/new-forms/components/EntityFormContainer.tsx';
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx';

const EntityFormContext = createContext<any | null>(null);

interface EntityFormContextProviderProps {
	openbisFacade: any;
	entityKind: string;
	user: string;
	permId: string;
	initialMode: FormMode;
	onEntityChange: (permId: string, changed: boolean) => void;
	onNewProject: () => void;
}

const EntityFormContextProvider = ({
	openbisFacade,
	entityKind,
	user,
	permId,
	initialMode,
	onEntityChange,
	onNewProject
}: EntityFormContextProviderProps) => {
	const [form, setForm] = React.useState<Form | null>(null)
	const [mode, setMode] = React.useState<FormMode | null>(initialMode)
	const [isAutoSaveEnabled, setAutoSaveEnabled] = useState(false);
	const [loadingStates, setLoadingStates] = useState({
		loaded: false,
		saving: false,
		updating: false,
		deleting: false
	});
	const [error, setError] = useState<{ state: boolean; error: string } | null>(null);
	const [loading, setLoading] = useState(false);

	// Map the entity kind string to our enum
	const mappedEntityKind = objectTypeToEntityKindMap[entityKind as keyof typeof objectTypeToEntityKindMap];
	if (!mappedEntityKind) {
		return <div>Unknown entity type: {entityKind}</div>;
	}

	const controller = useMemo(() => {
		switch (mappedEntityKind) {
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
				throw new Error(`Unsupported entity kind: ${mappedEntityKind}`);
		}
	}, [openbisFacade, user, mappedEntityKind]);

	const loadForm = useCallback(async () => {
		let mounted = true
		if (!loadingStates.loaded) {
			try {
				const form = await controller.load(permId)
				if (mounted) setForm(form)
			} catch (error: any) {
				if (mounted) setError({ state: true, error: error.message })
			} finally {
				if (mounted) setLoadingStates(prev => ({ ...prev, loaded: true }))
			}
		}
		return () => { mounted = false }
	}, [controller, permId])

	React.useEffect(() => {
		loadForm()
	}, [])

	const reloadForm = () => {
		setLoading(true);
		controller.load(permId)
			.then((f: Form) => {
				setForm(f);
				if (onEntityChange) onEntityChange(f.entityPermId, false);
			})
			.catch((e: any) => setError(e))
			.finally(() => setLoading(false));
	};

	const handleErrorCancel = () => {
		setError(null);
	};

	const handleEntityChange = (permId: string, changed: boolean) => {
		if (onEntityChange) onEntityChange(permId, changed);
	};

	return (
		<EntityFormContext.Provider value={{ controller, entityKind, mappedEntityKind, onEntityChange, onNewProject, 
		loadingStates, setLoadingStates, 
		error, setError, handleErrorCancel, handleEntityChange, 
		loading, setLoading, 
		form, setForm, 
		mode, setMode,
		isAutoSaveEnabled, setAutoSaveEnabled,
		reloadForm
		}}>
			<EntityFormContainer permId={permId} />
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

export default EntityFormContextProvider; 