import React, { useState } from 'react';
import { makeStyles } from '@mui/styles';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import SpaceFormView from '@src/js/components/database/new-forms/controllers/Space/SpaceFormView.tsx';
import ProjectFormView from '@src/js/components/database/new-forms/controllers/Project/ProjectFormView.tsx';
import SampleFormView from '@src/js/components/database/new-forms/controllers/Sample/SampleFormView.tsx';
import objectType from '@src/js/common/consts/objectType.js'
import { EntityFormBuilderProvider } from '@src/js/components/database/new-forms/components/EntityFormBuilderProvider.tsx';


const entityFormComponents = {
	[objectType.SPACE]: SpaceFormView,
	[objectType.PROJECT]: ProjectFormView,
	[objectType.SAMPLE]: SampleFormView,
};



const EntityFormContainer = ({ openbisFacade, entityKind, user, permId, onEntityChange, onNewProject }) => {
	const [loadingStates, setLoadingStates] = useState({
		initialLoad: true,
		saving: false,
		deleting: false
	});
	const error = { state: false, error: '404' };
	const loaded = true;
	const open = false;
	const handleErrorCancel = () => { }

	const handleEntityChange = () => {
		if (onEntityChange) onEntityChange(permId, true);
	}
	if (!loaded) return null;

	const EntityFormComponent = entityFormComponents[entityKind];
	if (!EntityFormComponent) return <div>Unknown entity type</div>;
	console.log('EntityFormContainer', { openbisFacade, entityKind, user, permId, onEntityChange });
	return (<>
		<LoadingDialog
			loading={open} />
		<ErrorDialog
			open={true}
			error={false}
			onClose={handleErrorCancel} />

		<EntityFormBuilderProvider openbisFacade={openbisFacade} entityKind={entityKind.toUpperCase()} user={user} onEntityChange={handleEntityChange} onNewProject={onNewProject}>
			<EntityFormComponent permId={permId} />
		</EntityFormBuilderProvider>
	</>
	);
};

export default EntityFormContainer;
