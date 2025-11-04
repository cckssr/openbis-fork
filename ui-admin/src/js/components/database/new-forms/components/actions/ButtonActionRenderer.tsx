import React from 'react';
import { ActionRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import Button from '@src/js/components/common/form/Button.jsx';

export const ButtonActionRenderer: React.FC<ActionRendererProps> = ({ action, onAction, mode }) => {
	
    return (
		//@ts-ignore
        <Button key={action.name}
                id={action.name}
                label={action.label}
                type={action.name.includes('save') ? 'final' : 'neutral'}
                onClick={() => onAction(action.name)}
              />
    );
};