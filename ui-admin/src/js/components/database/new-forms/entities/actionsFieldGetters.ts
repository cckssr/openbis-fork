import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

export const getSaveAction = (entityType?: EntityKind) => {
	const actionName = entityType ? `${entityType}:save` : 'save';
	return {
		name: actionName,
		label: 'Save',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: [FormMode.EDIT, FormMode.CREATE],
			},
		],
	};
};

export const getEditAction = () => {
	return {
		name: 'edit',
		label: 'Edit',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};
};

export const getCancelAction = (isNewForm: boolean = false) => {
	return {
		name: isNewForm ? 'cancelNewForm' : 'cancel',
		label: 'Cancel',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: [FormMode.EDIT, FormMode.CREATE],
			},
		],
	};
};

export const getAutoSaveAction = () => {
	return {
		name: 'auto-save',
		label: 'Auto-save',
		component: 'switch',
		isAllowed: true,
		visibility: [
			{
				mode: [FormMode.EDIT, FormMode.CREATE]
			},
		],
	};
};

export const getMoveAction = () => {
	return {
		name: 'move',
		label: 'Move',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};
};

export const getDeleteAction = () => {
	return {
		name: 'delete',
		label: 'Delete',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};
};

export const getNewProjectAction = () => {
	return {
		name: 'space:new-project',
		label: 'New Project',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};
};

export const getDividerAction = (onMode?: FormMode) => {
	// Generate unique name using timestamp + random to avoid React key conflicts
	// This ensures uniqueness without requiring manual counter resets
	const uniqueId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
	
	return {
		name: `divider-${uniqueId}`,
		label: 'Divider',
		component: 'divider',
		isAllowed: true,
		visibility: [
			{
				mode: onMode || FormMode.VIEW,
			},
		],
	};
};

export const getNewObjectAction = (entityType: EntityKind) => {
	return {
		name: `newObject`,
		label: 'New Object',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};
};

export const getNewCollectionAction = (entityType: EntityKind) => {
	return {
		name: `newCollection`,
		label: 'New Collection',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};	
};

export const getNewDatasetAction = (entityType: EntityKind) => {
	return {
		name: `newDataSet`,
		label: 'New Dataset',
		component: 'button',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
	};
};

export const getMoreActionsAction = () => {
	return {
		name: 'more-actions',
		label: 'More',
		component: 'dropdown',
		isAllowed: true,
		visibility: [
			{
				mode: FormMode.VIEW,
			},
		],
		meta: {
			items: [
				{ label: 'Export', actionName: 'export' },
				{ label: 'Freeze', actionName: 'freeze' }
			]
		}
	};
};