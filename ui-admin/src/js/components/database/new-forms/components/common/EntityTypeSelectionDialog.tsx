import React, { useState } from 'react';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import { Box } from '@mui/material';
import { EntityTypeSearchDropdown } from '@src/js/components/database/new-forms/components/common/EntityTypeSearchDropdown.tsx';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';

interface EntityTypeSelectionDialogProps {
	open: boolean;
	actionName: string;
	onConfirm: (entityType: any) => void;
	onCancel: () => void;
	openbisFacade?: any;
}

const EntityTypeSelectionDialog: React.FC<EntityTypeSelectionDialogProps> = ({ open, actionName, onConfirm, onCancel, openbisFacade }) => {
	const [selectedTarget, setSelectedTarget] = useState<any>(null);

	const handleTargetSelection = (target: any) => {
		setSelectedTarget(target);
		onConfirm(target);
	};

	const renderEntitySelection = () => {
		return (
			<Box sx={{ mt: 2 }}>
				<EntityTypeSearchDropdown
					openbisFacade={openbisFacade}
					actionName={actionName}
					onSelectionChange={handleTargetSelection}
					selectedEntity={selectedTarget}
				/>
			</Box>
		);
	};

	const getTitle = () => {
		switch (actionName) {
			case EntityKind.NEW_COLLECTION:
				return "Select a Collection type";
			case EntityKind.NEW_OBJECT:
				return "Select a Object type";
			case EntityKind.NEW_DATASET:
				return "Select a Dataset type";
			default:
				return "Select a Entity type";
		}
	};

	return (
		<Dialog
			open={open}
			onClose={onCancel}
			title={getTitle()}
			content={
				renderEntitySelection()
			}
			actions={<Button label="Cancel" onClick={onCancel} />}
		/>
	);
}

export default EntityTypeSelectionDialog;