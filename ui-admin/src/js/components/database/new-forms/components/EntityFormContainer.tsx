import React, { useState } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";

import { entityKindToFormComponentMap, objectTypeToEntityKindMap } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContextProvider.tsx';

const EntityFormContainer = ({ permId }: { permId: string }) => {

	const { entityKind, mappedEntityKind, onEntityChange, onNewProject, loadingStates, setLoadingStates, error, setError, loading, mode } = useEntityForm();

	const handleErrorCancel = () => {
		setError(null);
	};

	const handleEntityChange = (permId: string, changed: boolean) => {
		if (onEntityChange) onEntityChange(permId, changed);
	};


	const EntityFormComponent = entityKindToFormComponentMap[mappedEntityKind as keyof typeof entityKindToFormComponentMap];
	if (!EntityFormComponent) {
		return <div>No form component available for entity type: {entityKind}</div>;
	}

	const renderFormComponent = () => {
		const Component = entityKindToFormComponentMap[mappedEntityKind as keyof typeof entityKindToFormComponentMap];
		return <Component permId={permId} mode={mode} />;
	};

	if (!loadingStates.loaded) return null;
	return (
		<>
			<LoadingDialog loading={loading} />
			{/* @ts-ignore */}
			<ErrorDialog open={error?.state} error={error?.error} onClose={handleErrorCancel} />
			{renderFormComponent()}
		</>
	);
};

export default EntityFormContainer; 