// ============================================================================
// STATE MANAGEMENT: EntityFormContextProvider
// Description: holds the single source of truth for form state.
// It handles action execution by looking up handlers in the registry.
// ============================================================================

import React, { createContext, useContext, useState, useMemo, useEffect, useCallback } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import ControllerDispatcher from '@src/js/components/database/new-forms/engine/ControllerDispatcher.ts';
import ActionHandlerDispatcher from '@src/js/components/database/new-forms/engine/ActionHandlerDispatcher.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form, ActionContext } from '@src/js/components/database/new-forms/types/form.types.ts';
import { objectTypeToEntityKindMap } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx';
import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';

export const EntityFormContextProvider = ({ openbisFacade, params, entityKind, user, permId, initialMode, externalAppController }:
	{
		openbisFacade: any,
		params: any, 
		entityKind: string, 
		user: string, 
		permId: string, 
		initialMode: FormMode, 
		externalAppController: any
	}) => {
	const [form, setForm] = useState<Form | null>(null);
	const [initialForm, setInitialForm] = useState<Form | null>(null); // For 'cancel'
	const [mode, setMode] = useState<FormMode>(initialMode);
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<any>(null);
	const [permissions, setPermissions] = useState({ canEdit: true, canDelete: true, canMove: true });
	const [isAutoSaveEnabled, setAutoSaveEnabled] = useState(false);
	const [showConflictDialog, setShowConflictDialog] = useState(false);
	const [conflictFields, setConflictFields] = useState<any[]>([]);
	const [conflictResolutionActive, setConflictResolutionActive] = useState(false);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);
	const [deleteDialogConfig, setDeleteDialogConfig] = useState<any>(null);

	const { isConflicted, conflictingFields, resolveConflicts, checkModificationDateConflict, findConflicts } = useConflictResolution();

	/* const { saveToStorage, loadFromStorage, clearStorage } = useAutoSave({
		formData: form,
		storageKey: `form-data-${permId}`,
		isEnabled: isAutoSaveEnabled,
		interval: 5000,
	});

	useEffect(() => {
		if (isAutoSaveEnabled) {
			saveToStorage();
		}
	}, [isAutoSaveEnabled, form]); */

	/* const entityKind = objectTypeToEntityKindMap[entityKind as keyof typeof objectTypeToEntityKindMap];
	if (!entityKind) {
		return <div>Unknown entity type: {entityKind}</div>;
	} */

	// Create controller using dispatcher
	const controller = useMemo(
		() => ControllerDispatcher.createController(entityKind, openbisFacade, user),
		[entityKind, openbisFacade, user]
	);

	// Load initial form data
	useEffect(() => {
		reloadForm();
	}, [permId, controller]);

	const reloadForm = () => {
		console.log('reloadForm', { permId }, { entityKind }, { params });
		setLoading(true);
		controller.load(permId, entityKind, params)
			.then((loadedForm: Form) => {
				setForm(loadedForm);
				setInitialForm(loadedForm);
				setLoading(false);
			})
			.catch((e: any) => {
				setError({ state: true, error: e });
				setLoading(false);
			});
	};

	const handleErrorCancel = () => {
		setError(null);
	};
	// Handle field changes directly in the context
	const handleFieldChange = useCallback((fieldId: string, value: any) => {
		setForm(prevForm => {
			if (!prevForm) return null;
			return {
				...prevForm,
				fields: prevForm.fields.map(currentField => {
					if (currentField.id === fieldId) {
						console.log(`[EntityFormContextProvider] Updating field: ${currentField.id} to ${value}`);
						return { ...currentField, value };
					}
					return currentField;
				})
			};
		});
	}, []);

	// Handle actions by creating them from dispatcher
	const handleAction = useCallback(async (actionName: string) => {
		console.log(`[EntityFormContextProvider] Handling action: ${actionName}`);
		const actionHandler = ActionHandlerDispatcher.getActionHandler(actionName);
		if (actionHandler && form) {
			const context: ActionContext = {
				controller,
				form,
				setForm,
				mode,
				setMode,
				permissions,
				onAfterSave: (params?: any) => {
					console.log('[EntityFormContextProvider] context onAfterSave:', params);
					// Reload or update state after save
					setMode(FormMode.VIEW);
					if (params) {
						externalAppController.objectCreate(params);
					}
					reloadForm();
				},
				openbisFacade,
				externalAppController,
				isAutoSaveEnabled,
				setAutoSaveEnabled,
			};
			if (actionName.toLowerCase().includes('save')) {
				setSaving(true);
				setError(null);
				if (mode === FormMode.EDIT) {
					console.log(`[EntityFormContextProvider] Invoking EDIT action handler: ${actionHandler}`);
					try {
						const latestForm = await controller.load(form.entityPermId);
						if (!conflictResolutionActive && checkModificationDateConflict(form, latestForm)) {
							// Use resolveConflicts to get merged fields with conflict info
							const conflicts = findConflicts(form, latestForm) as any[];

							setConflictFields(conflicts);
							setShowConflictDialog(true);
							return;
						}
						// No conflicts, proceed to save
						const newVersion = await actionHandler(context); // await controller.save(form);
						//setForm(prev => ({ ...prev, version: newVersion }));
						//setMode(FormMode.VIEW);
						//setShowSuccess(true);
						setConflictResolutionActive(false);
						//if (onEntityChange) onEntityChange(form.entityPermId, false);
						if (context.onAfterSave) context.onAfterSave(params);
					} catch (error: any) {
						if (error.status === 409) {
							const latestForm = await controller.load(form.entityPermId);
							resolveConflicts(form, latestForm);
						} else {
							console.error('Save failed:', error);
							setError(error.message);
						}
					} finally {
						setSaving(false);
					}
				} else if (mode === FormMode.CREATE) {
					console.log(`[EntityFormContextProvider] Invoking CREATE action handler: ${actionHandler}`);
					await actionHandler(context);
					setSaving(false);
					/* try {
						await registeredActionHandler(context);
					} catch (e: any) {
						setError(e.message);
					} finally {
						setSaving(false);
					} */
				}
			} else if (actionName.toLowerCase().includes('edit')) {
				try {
					await actionHandler(context);
				} catch (e: any) {
					setError(e.message);
				} finally {
					setSaving(false);
				}
			} else if (actionName === 'cancel') {
				actionHandler(context);
				setForm(initialForm); // Reset to original state
			} else if (actionName === 'delete') {
				// Check for dependent entities before showing dialog
				handleDeleteWithDependencyCheck();
			} else {
				actionHandler(context);
			}
		} else {
			console.warn(`No action handler registered for '${actionName}'`);
		}
	}, [form, mode, permissions, openbisFacade, initialForm, externalAppController]);

	const handleResolveConflicts = (resolved: Record<string, any>) => {
		setForm(prevForm => {
			if (!prevForm) return null;
			return {
				...prevForm,
				fields: prevForm.fields.map(field =>
					resolved.hasOwnProperty(field.id) ? { ...field, value: resolved[field.id] } : field
				),
			};
		});
		setShowConflictDialog(false);
		setConflictResolutionActive(true);
		reloadForm();
	};

	const handleDeleteConfirm = async (reason: string) => {
		setShowDeleteDialog(false);
		setSaving(true);
		setError(null);
		
		try {
			const actionHandler = ActionHandlerDispatcher.getActionHandler('delete');
			if (actionHandler && form) {
				const context: ActionContext = {
					controller,
					form,
					setForm,
					mode,
					setMode,
					permissions,
					onAfterSave: (params?: any) => {
						console.log('[EntityFormContextProvider] context onAfterSave:', params);
						setMode(FormMode.VIEW);
						if (params) {
							externalAppController.objectCreate(params);
						}
						reloadForm();
					},
					openbisFacade,
					externalAppController,
					isAutoSaveEnabled,
					setAutoSaveEnabled,
					deleteReason: reason, // Pass the reason to the action handler
					dependentEntities: deleteDialogConfig?.dependentEntities, // Pass dependent entities info
				};
				await actionHandler(context);
				externalAppController.closeForm();
			}
		} catch (error: any) {
			console.error('Delete failed:', error);
			setError(error.message);
		} finally {
			setSaving(false);
		}
	};

	const handleDeleteCancel = () => {
		setShowDeleteDialog(false);
		setDeleteDialogConfig(null);
	};

	const handleDeleteWithDependencyCheck = async () => {
		if (!form || !controller) return;
		
		try {
			//setLoading(true);
			setError(null);
			
			// First check for existing deletions in trashcan
			try {
				await controller.delete(form, { checkOnly: true });
			} catch (deletionError: any) {
				// If there are existing deletions, show error and don't proceed
				setError(deletionError.message);
				return;
			}
			
			// Check for dependent entities using the controller's method
			const dependentEntities = await controller.getDependentEntities(form);
			const totalDependentEntities = dependentEntities.experiments.length + dependentEntities.samples.length;
			
			// Simplified configuration - let the dialog handle text generation
			setDeleteDialogConfig({
				includeReason: true,
				numberOfEntities: totalDependentEntities, // +1 for the main entity
				bypassesTrashcan: false,
				dependentEntities: dependentEntities,
				entityKind: entityKind
			});

			/* warningText={deleteDialogConfig.warningText}
			includeReason={deleteDialogConfig.includeReason}
			numberOfEntities={deleteDialogConfig.numberOfEntities}
			bypassesTrashcan={deleteDialogConfig.bypassesTrashcan}
			additionalText={deleteDialogConfig.additionalText}
			customPlugin={deleteDialogConfig.customPlugin}
			dependentEntities={deleteDialogConfig.dependentEntities}
			entityKind={deleteDialogConfig.entityKind} */

			setShowDeleteDialog(true);
		} catch (error: any) {
			console.error('Error checking dependencies:', error);
			setError(error.message);
		} finally {
			setLoading(false);
		}
	};

	if (loading) return <LoadingDialog loading={loading} />;
	// @ts-ignore
	if (error?.state) return <ErrorDialog open={error?.state} error={error?.error} onClose={handleErrorCancel} />;
	if (!form) return null;

	return (
		<EntityForm
			form={form}
			mode={mode}
			permissions={permissions}
			onFieldChange={handleFieldChange}
			onAction={handleAction}
			isSaving={saving}
			error={error}
			showConflictDialog={showConflictDialog}
			conflictFields={conflictFields}
			handleResolveConflicts={handleResolveConflicts}
			setShowConflictDialog={setShowConflictDialog}
			showDeleteDialog={showDeleteDialog}
			deleteDialogConfig={deleteDialogConfig}
			onDeleteConfirm={handleDeleteConfirm}
			onDeleteCancel={handleDeleteCancel}
			onErrorClose={handleErrorCancel}
		/>
	);
};