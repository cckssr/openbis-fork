import React, { useState } from 'react';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import Message from '@src/js/components/common/form/Message.jsx';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';
import {
	Autocomplete,
	TextField,
	Box,
	Typography,
	CircularProgress
} from '@mui/material';
import { EntityTypeSearchDropdown } from '@src/js/components/database/new-forms/components/common/EntityTypeSearchDropdown.tsx';

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
		console.log({target});
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

	return (
		<Dialog
			open={open}
			onClose={onCancel}
			title="Select a Experiment/Collection type"
			content={
				renderEntitySelection()
			}
			actions={<Button label="Cancel" onClick={onCancel} />}
		/>
	);
}

export default EntityTypeSelectionDialog;