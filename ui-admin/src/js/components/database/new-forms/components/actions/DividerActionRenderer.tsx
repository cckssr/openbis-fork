import React from 'react';
import { ActionRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { Divider } from '@mui/material';

export const DividerActionRenderer: React.FC<ActionRendererProps> = ({ action, onAction, mode }) => {
	
    return (
		<Divider orientation="vertical" variant="middle" />
    );
};