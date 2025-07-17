// ============================================================================
// 4. STATE MANAGEMENT: EntityFormContextProviderV2.tsx (REFACTORED)
// Description: Now holds the single source of truth for form state.
// It handles action execution by looking up handlers in the registry.
// ============================================================================
import React, { createContext, useContext, useState, useMemo, useEffect, useCallback } from 'react';
import EntityFormV2 from '@src/js/components/database/new-forms/components/EntityFormV2.tsx';
import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { Form, FormMode, ActionContext } from '@src/js/components/database/new-forms/types/form.types.ts';
//import EntityFormContainerV2 from '@src/js/components/database/new-forms/components/EntityFormContainerV2.tsx';
import { objectTypeToEntityKindMap } from '@src/js/components/database/new-forms/utils/Utils.ts';

const EntityFormContext = createContext<any>(null);

export const EntityFormContextProviderV2 = ({ openbisFacade, entityKind, user, permId, initialMode, onEntityChange, onNewProject, onCloseForm }:
	{
		openbisFacade: any, entityKind: string, user: string, permId: string, initialMode: FormMode,
		onEntityChange: (permId: string, isNew: boolean) => void,
		onNewProject: () => void,
		onCloseForm: () => void
	}) => {
	const [form, setForm] = useState<Form | null>(null);
	const [initialForm, setInitialForm] = useState<Form | null>(null); // For 'cancel'
	const [mode, setMode] = useState<FormMode>(initialMode);
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<any>(null);
	const [permissions, setPermissions] = useState({ canEdit: true, canDelete: true, canMove: true });

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
		setLoading(true);
		controller.load(permId, mappedEntityKind)
			.then((loadedForm: Form) => {
				setForm(loadedForm);
				setInitialForm(loadedForm); // Store the pristine version
				setLoading(false);
			})
			.catch((e: any) => {
				setError({ state: true, error: e.message });
				setLoading(false);
			});
	}, [permId, controller]);

	const reloadForm = () => {
		setLoading(true);
		controller.load(permId)
			.then((f: Form) => {
				setForm(f as Form);
				setLoading(false);
			})
			.catch((e: any) => {
				setError({ state: true, error: e.message });
				setLoading(false);
			});
	};
	// Handle field changes directly in the context
	const handleFieldChange = useCallback((fieldId: string, value: any) => {
		console.log(`[EntityFormContextProviderV2] Handling field change: ${fieldId} to ${value}`);
		setForm(prevForm => {
			console.log(`[EntityFormContextProviderV2] Prev form: ${prevForm}`);
			if (!prevForm) return null;
			return {
				...prevForm,
				fields: prevForm.fields.map(currentField => {
					if (currentField.id === fieldId) {
						console.log(`[EntityFormContextProviderV2] Updating field: ${currentField.id} to ${value}`);
						return { ...currentField, value };
					}
					return currentField;
				})
			};
		});
	}, []);

	// Handle actions by looking them up in the registry
	const handleAction = useCallback(async (actionName: string) => {
		console.log(`[EntityFormContextProviderV2] Handling action: ${actionName}`);
		const handler = FormEngineRegistry.getAction(actionName);
		console.log(`[EntityFormContextProviderV2] Handler: ${handler}`);
		if (handler && form) {
			const context: ActionContext = {
				controller,
				form,
				setForm,
				mode,
				setMode,
				permissions,
				onAfterSave: () => {
					// Reload or update state after save
					setMode(FormMode.VIEW);
				},
				openbisFacade,
				onNewProject,
				onEntityChange,
				closeForm: onCloseForm,
			};

			// Special handling for actions that modify saving state
			if (actionName.toLowerCase().includes('save')) {
				setSaving(true);
				setError(null);
				try {
					await handler(context);
				} catch (e: any) {
					setError(e.message);
				} finally {
					setSaving(false);
				}
			} else if (actionName.toLowerCase().includes('edit')) {
				try {
					await handler(context);
				} catch (e: any) {
					setError(e.message);
				} finally {
					setSaving(false);
				}
			} else if (actionName === 'cancel') {
				handler(context);
				setForm(initialForm); // Reset to original state
			}
			else {
				handler(context);
			}
		} else {
			console.warn(`No action handler registered for '${actionName}'`);
		}
	}, [form, mode, permissions, openbisFacade, initialForm, onNewProject, onEntityChange]);

	if (loading) return <div>Loading...</div>;
	if (error?.state) return <div>Error: {error.error}</div>;
	if (!form) return null;

	return (
		// No need for a separate context provider, just pass props to the dumb component
		<EntityFormV2
			form={form}
			mode={mode}
			permissions={permissions}
			onFieldChange={handleFieldChange}
			onAction={handleAction}
			isSaving={saving}
			error={error}
		/>
	);

	/* return (
		<EntityFormContext.Provider value={{
			controller, form, setForm,
			mode, setMode, loading,
			setLoading, error, setError, reloadForm, permissions, onEntityChange, onNewProject, mappedEntityKind
		}}>
			<EntityFormContainerV2 permId={permId} />
		</EntityFormContext.Provider>
	); */
};

/* export const useEntityForm = () => {
	const ctx = useContext(EntityFormContext);
	if (!ctx) throw new Error('useEntityForm must be used within EntityFormContextProviderV2');
	return ctx;
};  */