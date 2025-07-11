import React, { useState } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import SpaceFormView from '@src/js/components/database/new-forms/controllers/Space/SpaceFormView.tsx';
import ProjectFormView from '@src/js/components/database/new-forms/controllers/Project/ProjectFormView.tsx';
import ObjectFormView from '@src/js/components/database/new-forms/controllers/Object/ObjectFormView.tsx';
import CollectionFormView from '@src/js/components/database/new-forms/controllers/Collection/CollectionFormView.tsx';
import { ProjectCreationForm } from '@src/js/components/database/new-forms/controllers/Project/ProjectCreationForm.tsx';
import objectType from '@src/js/common/consts/objectType.js';
import { EntityFormProvider } from '@src/js/components/database/new-forms/components/EntityFormProvider.tsx';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';
import DatasetFormView from '@src/js/components/database/new-forms/controllers/Dataset/DatasetFormView.tsx';

// Map objectType constants to EntityKind enum
const entityKindMap = {
	[objectType.SPACE]: EntityKind.SPACE,
	[objectType.PROJECT]: EntityKind.PROJECT,
	[objectType.NEW_PROJECT]: EntityKind.NEW_PROJECT,
	[objectType.OBJECT]: EntityKind.SAMPLE,
	[objectType.COLLECTION]: EntityKind.COLLECTION,
	[objectType.DATA_SET]: EntityKind.DATASET,
	// Note: SAMPLE is not in objectType.js, so we'll handle it separately
};

// Map entity kinds to their respective form components
const entityFormComponents = {
	[EntityKind.SPACE]: SpaceFormView,
	[EntityKind.PROJECT]: ProjectFormView,
	[EntityKind.NEW_PROJECT]: ProjectCreationForm,
	[EntityKind.SAMPLE]: ObjectFormView,
	[EntityKind.COLLECTION]: CollectionFormView,
	[EntityKind.DATASET]: DatasetFormView
};

interface EntityFormContainerProps {
	openbisFacade: any;
	entityKind: string;
	user: string;
	permId: string;
	onEntityChange: (permId: string, changed: boolean) => void;
	onNewProject: () => void;
}

const EntityFormContainer = ({ 
	openbisFacade, 
	entityKind, 
	user, 
	permId, 
	onEntityChange, 
	onNewProject 
}: EntityFormContainerProps) => {
	const [loadingStates, setLoadingStates] = useState({
		initialLoad: true,
		saving: false,
		updating: false,
		deleting: false
	});
	const [error, setError] = useState<{ state: boolean; error: string } | null>(null);
	const [loading, setLoading] = useState(false);

	const handleErrorCancel = () => {
		setError(null);
	};

	const handleEntityChange = (permId: string, changed: boolean) => {
		if (onEntityChange) onEntityChange(permId, changed);
	};
	

	// Map the entity kind string to our enum
	const mappedEntityKind = entityKindMap[entityKind as keyof typeof entityKindMap];
	if (!mappedEntityKind) {
		return <div>Unknown entity type: {entityKind}</div>;
	}

	const EntityFormComponent = entityFormComponents[mappedEntityKind as keyof typeof entityFormComponents];
	if (!EntityFormComponent) {
		return <div>No form component available for entity type: {entityKind}</div>;
	}

	const renderFormComponent = () => {
		const Component = entityFormComponents[mappedEntityKind as keyof typeof entityFormComponents];
		return <Component permId={permId} />;
	};

	return (
		<>
			<LoadingDialog loading={loading} />
			{error && error.state && (
				// @ts-ignore
				<ErrorDialog open={error.state} error={error.error} onClose={handleErrorCancel}/>
			)}

			<EntityFormProvider 
				openbisFacade={openbisFacade} 
				entityKind={mappedEntityKind} 
				user={user} 
				onEntityChange={handleEntityChange} 
				onNewProject={onNewProject}
			>
				{renderFormComponent()}
			</EntityFormProvider>
		</>
	);
};

export default EntityFormContainer; 