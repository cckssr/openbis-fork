// ============================================================================
// STATE MANAGEMENT: EntityFormContextProvider
// Description: holds the single source of truth for form state.
// It handles action execution by looking up handlers in the registry.
// ============================================================================

import React, { createContext, useContext, useState, useMemo, useEffect, useCallback } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { Form, FormMode, ActionContext } from '@src/js/components/database/new-forms/types/form.types.ts';
import { objectTypeToEntityKindMap } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx';
import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';

export const EntityFormContextProvider = ({ openbisFacade, entityKind, user, permId, initialMode, params, onEntityChange, onNewObject, onCloseForm, onObjectCreate }:
	{
		openbisFacade: any, entityKind: string, user: string, permId: string, initialMode: FormMode, params: any,
		onEntityChange: (permId: string, isNew: boolean) => void,
		onNewObject: (newObjectType: string, fromId: string) => void,
		onCloseForm: () => void,
		onObjectCreate: (page: string, oldType: string, oldId: string, newType: string, newId: string) => void
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

	const mappedEntityKind = objectTypeToEntityKindMap[entityKind as keyof typeof objectTypeToEntityKindMap];
	if (!mappedEntityKind) {
		return <div>Unknown entity type: {entityKind}</div>;
	}

	// The controller is now simpler, mainly used for loading data
	const controller = useMemo(
		() => FormEngineRegistry.getController(mappedEntityKind, openbisFacade, user),
		[mappedEntityKind, openbisFacade, user]
	);

	// Load initial form data
	useEffect(() => {
		reloadForm();
	}, [permId, controller]);

	const reloadForm = () => {
		console.log('reloadForm', { permId }, { mappedEntityKind }, { params });
		setLoading(true);
		controller.load(permId, mappedEntityKind, params)
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
		console.log(`[EntityFormContextProvider] Handling field change: ${fieldId} to ${value}`);
		setForm(prevForm => {
			console.log(`[EntityFormContextProvider] Prev form: ${prevForm}`);
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

	// Handle actions by looking them up in the registry
	const handleAction = useCallback(async (actionName: string) => {
		console.log(`[EntityFormContextProvider] Handling action: ${actionName}`);
		const registeredActionHandler = FormEngineRegistry.getAction(actionName);
		console.log(`[EntityFormContextProvider] Registered action handler: ${registeredActionHandler}`);
		if (registeredActionHandler && form) {
			const context: ActionContext = {
				controller,
				form,
				setForm,
				mode,
				setMode,
				permissions,
				onAfterSave: (params?: any) => {
					console.log('onAfterSave');
					// Reload or update state after save
					setMode(FormMode.VIEW);
					if (params) {
						onObjectCreate(params.page, params.oldType, params.oldId, params.newType, params.newId);
					}
					reloadForm();
				},
				openbisFacade,
				onNewObject,
				onEntityChange,
				closeForm: onCloseForm,
				isAutoSaveEnabled,
				setAutoSaveEnabled,
			};
			console.log(`[EntityFormContextProvider] Context: ${context}`);
			// Special handling for actions that modify saving state
			if (actionName.toLowerCase().includes('save')) {
				setSaving(true);
				setError(null);
				if (mode === FormMode.EDIT) {
					console.log(`[EntityFormContextProvider] Invoking EDIT action handler: ${registeredActionHandler}`);
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
						const newVersion = await registeredActionHandler(context); // await controller.save(form);
						//setForm(prev => ({ ...prev, version: newVersion }));
						//setMode(FormMode.VIEW);
						//setShowSuccess(true);
						setConflictResolutionActive(false);
						//if (onEntityChange) onEntityChange(form.entityPermId, false);
						if (context.onAfterSave) context.onAfterSave();
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
					console.log(`[EntityFormContextProvider] Invoking CREATE action handler: ${registeredActionHandler}`);
					await registeredActionHandler(context);
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
					await registeredActionHandler(context);
				} catch (e: any) {
					setError(e.message);
				} finally {
					setSaving(false);
				}
			} else if (actionName === 'cancel') {
				registeredActionHandler(context);
				setForm(initialForm); // Reset to original state
			}
			else {
				registeredActionHandler(context);
			}
		} else {
			console.warn(`No action handler registered for '${actionName}'`);
		}
	}, [form, mode, permissions, openbisFacade, initialForm, onNewObject, onEntityChange]);

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
		/>
	);
};