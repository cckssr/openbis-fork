import React, { useState, useMemo, useEffect, useCallback } from 'react';
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import ErrorDialog from "@src/js/components/common/error/ErrorDialog.jsx";
import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import ControllerDispatcher from '@src/js/components/database/new-forms/engine/ControllerDispatcher.ts';
import ActionHandlerDispatcher from '@src/js/components/database/new-forms/engine/ActionHandlerDispatcher.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/form.types.ts';
import { useConflictResolution } from '@src/js/components/database/new-forms/hooks/useConflictResolution.tsx';
import ConflictResolutionDialog from '@src/js/components/database/new-forms/components/ConflictResolutionDialog.tsx';
import DeleteConfirmationDialog from '@src/js/components/database/new-forms/components/DeleteConfirmationDialog.tsx';
import { useFormState } from '@src/js/components/database/new-forms/hooks/useFormState.ts';

export const EntityFormContextProvider = ({ openbisFacade, params, entityKind, user, sessionID, permId, initialMode, externalAppController }:
	{
		openbisFacade: any,
		params: any,
		entityKind: string,
		user: string,
		sessionID: string,
		permId: string,
		initialMode: FormMode,
		externalAppController: any
	}) => {
	const { form, mode, setForm, setMode, updateField } = useFormState({ initialForm: null, initialMode });
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

	const getExtendedActionContext = useCallback((reason?: string): IExtendedActionContext => {
		if (!form) throw new Error('Form is not loaded');
		return {
			controller,
			form,
			setForm,
			mode,
			setMode,
			onAfterSave: (params?: any) => {
				console.log('[EntityFormContextProvider] context onAfterSave:', params);
				// Reload or update state after save
				setMode(FormMode.VIEW);
				if (params) {
					externalAppController.objectCreate(params);
				}
				reloadForm();
			},
			externalAppController,
			deleteReason: reason || undefined,
			dependentEntities: deleteDialogConfig?.dependentEntities || undefined,
		}
	}, [form, mode, externalAppController]);

	// Load initial form data
	useEffect(() => {
		reloadForm();
	}, [permId, controller]);

	const reloadForm = () => {
		console.log('reloadForm', { permId }, { entityKind }, { params });
		setLoading(true);
		setError(null);
		if (entityKind === EntityKind.NEW_OBJECT) {
			controller.load(permId, entityKind, params, 'ENTRY')
				.then((loadedForm: Form) => {
					setForm(loadedForm);
					setLoading(false);
				})
				.catch((e: any) => {
					setError({ state: true, error: e });
					setLoading(false);
				});
		} else {
			controller.load(permId, entityKind, params)
			.then((loadedForm: Form) => {
				setForm(loadedForm);
				setLoading(false);
			})
			.catch((e: any) => {
				setError({ state: true, error: e });
				setLoading(false);
			});
		}
	};

	const handleErrorCancel = () => {
		setError(null);
	};

	// Handle actions by creating them from dispatcher
	const handleAction = useCallback(async (actionName: string) => {
		console.log(`[EntityFormContextProvider] Handling action: ${actionName}`);
		const actionHandler = ActionHandlerDispatcher.getActionHandler(actionName);
		if (actionHandler && form) {
			if (actionName.toLowerCase().includes('save')) {
				handleSaveActions(actionHandler);
			} else if (actionName === 'delete') {
				// Check for dependent entities before showing dialog
				handleDeleteWithDependencyCheck();
			} else {
				const context: IExtendedActionContext = getExtendedActionContext();
				try {
					await actionHandler(context);
				} catch (e: any) {
					setError(e.message);
				} finally {
					setSaving(false);
				}
			}
		} else {
			console.warn(`No action handler registered for '${actionName}'`);
		}
	}, [form, mode, permissions, openbisFacade, externalAppController]);

	const handleResolveConflicts = async (resolved: Record<string, any>) => {
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
	};

	const handleSaveActions = async (actionHandler: any) => {
		if (!form) throw new Error('Form is not loaded');
		const context: IExtendedActionContext = getExtendedActionContext();
		setSaving(true);
		setError(null);
		if (mode === FormMode.EDIT) {
			try {
				const latestForm = await controller.load(form.entityPermId);
				if (!conflictResolutionActive && checkModificationDateConflict(form, latestForm)) {
					const conflicts = findConflicts(form, latestForm) as any[];
					setConflictFields(conflicts);
					setShowConflictDialog(true);
					return;
				}
				await actionHandler(context);
				setConflictResolutionActive(false);
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
			try {
				await actionHandler(context);
			} catch (error: any) {
				setError(error.message);
			} finally {
				setSaving(false);
			}
			setSaving(false);
		} else {
			throw new Error('Invalid mode');
		}
	};

	const handleDeleteConfirm = async (reason: string) => {
		setShowDeleteDialog(false);
		setSaving(true);
		setError(null);

		if (form) {
			const context: IExtendedActionContext = getExtendedActionContext(reason);
			try {
				await controller.delete(form, context);
			} catch (error: any) {
				setError(error.message);
			} finally {
				setSaving(false);
			}
			externalAppController.closeForm({ type: context.form.entityType, id: context.form.entityPermId });
		}
	};

	const handleDeleteCancel = () => {
		setShowDeleteDialog(false);
		setDeleteDialogConfig(null);
	};

	const handleDeleteWithDependencyCheck = async () => {
		if (!form || !controller) return;
		try {
			setLoading(true);
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
			console.log('handleDeleteWithDependencyCheck.dependentEntities:', dependentEntities);
			const totalDependentEntities = dependentEntities.experiments.length + dependentEntities.samples.length;

			// Simplified configuration - let the dialog handle text generation
			setDeleteDialogConfig({
				includeReason: true,
				numberOfEntities: totalDependentEntities, // +1 for the main entity
				bypassesTrashcan: totalDependentEntities === 0,
				dependentEntities: dependentEntities,
				entityKind: entityKind
			});

			setShowDeleteDialog(true);
		} catch (error: any) {
			console.error('Error checking dependencies:', error);
			setError(error.message);
		} finally {
			setLoading(false);
		}
	};

	if (loading) return <LoadingDialog loading={loading} />;
	if (!form) return null;

	return (
		<>
			{saving && <LoadingDialog loading={saving} />}
			{//@ts-ignore
				error && <ErrorDialog key='entity-form-error-dialog' open={!!error} error={error} onClose={handleErrorCancel} />}
			<EntityForm
				form={form}
				mode={mode}
				permissions={permissions}
				onFieldChange={updateField}
				onAction={handleAction}
				params={{ sessionID: sessionID }}
			/>
			{showConflictDialog && (
				<ConflictResolutionDialog
					open={showConflictDialog}
					conflicts={conflictFields}
					onResolve={handleResolveConflicts}
					onCancel={() => setShowConflictDialog(false)}
				/>
			)}
			{showDeleteDialog && deleteDialogConfig && (
				<DeleteConfirmationDialog
					open={showDeleteDialog}
					onConfirm={handleDeleteConfirm}
					onCancel={handleDeleteCancel}
					config={deleteDialogConfig}
				/>
			)}
		</>
	);
};