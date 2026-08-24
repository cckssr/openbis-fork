import React, { useState } from 'react';
import { ActionRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import Button from '@src/js/components/common/form/Button.jsx';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';

interface DropdownMenuItem {
	label: string;
	actionName: string;
}

export const DropdownActionRenderer: React.FC<ActionRendererProps> = ({ action, onAction, mode }) => {
	const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
	const open = Boolean(anchorEl);

	const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
		setAnchorEl(event.currentTarget);
	};

	const handleClose = () => {
		setAnchorEl(null);
	};

	const handleMenuItemClick = (actionName: string) => {
		onAction(actionName);
		handleClose();
	};

	// Get menu items from action metadata
	const menuItems: DropdownMenuItem[] = (action as any).meta?.items;

	return (
		<>
			<Button key={action.name}
				id={action.name}
				label={action.label}
				color='white'
				onClick={handleClick}
				endIcon={<KeyboardArrowDownIcon />}
				sx={{
					minWidth: 'auto'
				}}
			/>
			<Menu
				anchorEl={anchorEl}
				open={open}
				onClose={handleClose}
				anchorOrigin={{
					vertical: 'bottom',
					horizontal: 'left',
				}}
				transformOrigin={{
					vertical: 'top',
					horizontal: 'left',
				}}
			>
				{menuItems.map((item) => (
					<MenuItem 
						key={item.actionName}
						onClick={() => handleMenuItemClick(item.actionName)}
					>
						{item.label}
					</MenuItem>
				))}
			</Menu>
		</>
	);
};