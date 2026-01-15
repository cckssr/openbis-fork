import React from 'react';
import { ActionRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import Button from '@src/js/components/common/form/Button.jsx';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Cancel';


export const ButtonActionRenderer: React.FC<ActionRendererProps> = ({ action, onAction, mode }) => {
	
    const startIcon = action.name.includes('delete') ? <DeleteIcon /> : 
    action.name.includes('edit') ? <EditIcon /> : 
    action.name.includes('save') ? <SaveIcon /> : 
    action.name.includes('cancel') ? <CancelIcon /> :
    action.name.includes('new') ? <AddIcon /> : undefined;

    return (
		//@ts-ignore
        <Button key={action.name}
                id={action.name}
                label={action.label}
                type={action.name.includes('save') ? 'final' : 'neutral'}
                onClick={() => onAction(action.name)}
                startIcon={startIcon}
              />
    );
};