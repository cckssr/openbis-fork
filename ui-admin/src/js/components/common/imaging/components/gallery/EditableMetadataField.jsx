import React from "react";
import { FormControl, IconButton, Stack, TextareaAutosize } from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import TextField from '@src/js/components/common/form/TextField.jsx'

const EditableMetadataField = ({ keyProp, valueProp, onEdit }) => {
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
			(<div style={{ width: '100%' }}>
				<TextField label={keyProp}
					fullWidth
					value={valueProp}
					variant='standard'
					mode='view'/>
			</div>
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