import React from "react";
import { FormControl, IconButton, Stack, TextareaAutosize, Typography } from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import CancelIcon from "@mui/icons-material/Cancel";
import TextField from '@src/js/components/common/form/TextField.jsx'
import TextAreaField from '@src/js/components/common/form/TextAreaField.jsx'

const EditableMetadataField = ({ keyProp, valueProp, onEdit }) => {
	const [editMode, setEditMode] = React.useState(false);
	const [editableValue, setEditableValue] = React.useState(valueProp || "");

	// Update editableValue when valueProp changes (e.g., when switching previews)
	React.useEffect(() => {
		if (!editMode) {
			setEditableValue(valueProp || "");
		}
	}, [valueProp, editMode]);

	const toggleEditMode = () => {
		if (!editMode) {
			// Entering edit mode: reset to current valueProp
			setEditableValue(valueProp || "");
		}
		setEditMode(!editMode);
	}

	const saveComment = () => {
		setEditMode(false);
		onEdit(editableValue);
	}

	const cancelEdit = () => {
		setEditMode(false);
		setEditableValue(valueProp || "");
	}

	// Use editableValue when in edit mode, valueProp when in view mode
	const displayValue = editMode ? editableValue : (valueProp || "");

	return <Stack direction='row'>
		<div style={{ width: '100%' }}>
			<TextAreaField
				name={editMode ? 'text-area-comment' : ''}
				label={keyProp}
				fullWidth
				value={displayValue}
				placeholder="Add a comment"
				variant='standard'
				mode={editMode ? 'edit' : 'view'}
				onChange={event => setEditableValue(event.target.value)}
				styles={{}}
			/>
		</div>

		{!editMode && (
			<IconButton aria-label="edit" size="small" color="secondary"
				onClick={toggleEditMode}>
				<EditIcon />
			</IconButton>
		)}
		{editMode && (
			<>
				<IconButton aria-label="save" size="small" color="primary" onClick={saveComment}>
					<SaveIcon />
				</IconButton>
				<IconButton aria-label="cancel" size="small" color="secondary" onClick={cancelEdit}>
					<CancelIcon />
				</IconButton>
			</>
		)}
	</Stack>
}

export default EditableMetadataField;