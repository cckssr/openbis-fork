import React from "react";
import { IconButton, TextareaAutosize } from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";

const EditableMetadataField = ({ keyProp, valueProp, idx, onEdit }) => {
	const [saveMode, setSaveMode] = React.useState(false);
	const [editableValue, setEditableValue] = React.useState("");

	React.useEffect(() => {
		setEditableValue(valueProp);
	}, [])

	const handleEditedField = (event) => {
		if (!saveMode) setSaveMode(true);
		setEditableValue(event.target.value);
	}

	const saveComment = () => {
		setSaveMode(false);
		onEdit(editableValue);
	}

	return (
		<p key={'metadata-' + keyProp + '-' + idx}>
			{saveMode && <IconButton aria-label="save" size="small" disabled={!saveMode}
				color="primary" onClick={saveComment}>
				<SaveIcon />
			</IconButton>}
			<strong> {keyProp}: </strong>
			<TextareaAutosize maxRows={4}
				placeholder="Add a comment"
				value={editableValue}
				onChange={handleEditedField}/>
		</p>
	)
}

export default EditableMetadataField;