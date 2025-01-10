import React from "react";
import { FormControl, IconButton, InputLabel, Stack, TextareaAutosize, TextField } from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SaveIcon from "@mui/icons-material/Save";

const EditableMetadataField = ({ keyProp, valueProp, idx, onEdit }) => {
	const [editMode, setEditMode] = React.useState(false);
	const [editableValue, setEditableValue] = React.useState("");

	React.useEffect(() => {
		setEditableValue(valueProp);
	}, [])

	const toggleEditMode = () => {
		setEditMode(!editMode);
	}

	const saveComment = () => {
		setEditMode(false);
		onEdit(editableValue);
	}

	return <Stack direction='row'>
		{!editMode ? 
			(<TextField label={keyProp}
				value={valueProp}
				variant='standard'
				fullWidth
				sx={{my: 1}}
				slotProps={{
					input: {
						readOnly: true,
					},
					inputLabel: {
						disableAnimation: true,
						sx: { fontWeight: 'bold', color: 'black' }
					}
				}}/>
			) :
			(<FormControl>
				<strong> {keyProp}: </strong>
				<TextareaAutosize name='text-area-comment'
					placeholder="Add a comment"
					value={editableValue}
					onChange={event => setEditableValue(event.target.value)} />
			</FormControl>
		)}
		<IconButton aria-label="edit" size="small" color="primary"
			onClick={toggleEditMode}>
			<EditIcon />
		</IconButton>
		<IconButton aria-label="save" size="small" sx={{ display: editMode ? 'unset' : 'none' }}
			color="primary" onClick={saveComment}>
			<SaveIcon />
		</IconButton>
	</Stack>
}

export default EditableMetadataField;