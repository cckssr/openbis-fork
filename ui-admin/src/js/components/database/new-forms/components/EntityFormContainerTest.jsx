import React from 'react';
import { makeStyles } from '@mui/styles';
import objectType from '@src/js/common/consts/objectType.js'
import { SpaceFormContainer } from '@src/js/components/database/new-forms/entities/space/index.ts';
import { ProjectFormContainer } from '@src/js/components/database/new-forms/entities/project/index.ts';

const EntityFormContainer = ({ 
	openbisFacade, 
	entityKind, 
	user, 
	permId,
	// External callbacks from parent
	onSaved,
	onDeleted,
	onError,
	onNavigate
}) => {

	// Route to the appropriate entity container based on entityKind
	const renderEntityForm = () => {
		switch (entityKind) {
			case objectType.SPACE:
				return (
					<SpaceFormContainer
						permId={permId}
						openbisFacade={openbisFacade}
						user={user}
						onSaved={onSaved}
						onDeleted={onDeleted}
						onError={onError}
						onNavigate={onNavigate}
					/>
				);
			
			case objectType.PROJECT:
				return (
					<ProjectFormContainer
						permId={permId}
						openbisFacade={openbisFacade}
						user={user}
						onSaved={onSaved}
						onDeleted={onDeleted}
						onError={onError}
						onNavigate={onNavigate}
					/>
				);
			
			default:
				return <div>Unknown entity type: {entityKind}</div>;
		}
	};

	return (
		<>
			{renderEntityForm()}
		</>
	);
};

export default EntityFormContainer;
