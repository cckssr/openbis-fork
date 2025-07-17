import React from 'react'
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContextProvider.tsx'
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx'
import { FormMode, Form, FormAction } from '@src/js/components/database/new-forms/types/form.types.ts'
import Button from '@src/js/components/common/form/Button.jsx'
import { Stack } from '@mui/material';

const ProjectFormView = ({ permId }: { permId: string }) => {
	const {controller, onEntityChange} = useEntityForm()
	const [form, setForm] = React.useState<any>(null)
	const [loading, setLoading] = React.useState(true)
	const [error, setError] = React.useState<any>(null)

	React.useEffect(() => {
		let mounted = true
		setLoading(true)
		controller.load(permId)
			.then((f: any) => { if (mounted) setForm(f) })
			.catch((e: any) => { if (mounted) setError(e) })
			.finally(() => { if (mounted) setLoading(false) })
		return () => { mounted = false }
	}, [controller, permId])

	const reloadForm = () => {
		setLoading(true);
		controller.load(permId)
			.then((f: any) => {
				setForm(f);
				if (onEntityChange) onEntityChange(f.entityPermId, false);
			})
			.catch((e: any) => setError(e))
			.finally(() => setLoading(false));
	};

	// Define actions directly with booleans
	/* const actions: FormAction[] = [
		{
			name: 'edit',
			label: 'Edit',
			handler: (form, controller) => { console.log('Edit action'); },
			isAllowed: true,
			isVisible: form && form.mode === FormMode.VIEW
		},
		{
			name: 'save',
			label: 'Save',
			handler: (form, controller) => { console.log('Save action'); },
			isAllowed: true,
			isVisible: form && form.mode === FormMode.EDIT
		},
		{
			name: 'delete',
			label: 'Delete',
			handler: (form, controller) => { console.log('Delete action'); },
			isAllowed: true,
			isVisible: true
		},
		{
			name: 'move',
			label: 'Move',
			handler: (form, controller) => { console.log('Move action'); },
			isAllowed: true,
			isVisible: true
		},
		{
			name: 'newProject',
			label: '+ Project',
			handler: (form, controller) => { console.log('New Project action'); },
			isAllowed: true,
			isVisible: true
		},
		{
			name: 'more',
			label: 'More...',
			handler: (form, controller) => { console.log('More... action'); },
			isAllowed: true,
			isVisible: true
		}
	]; */

	if (loading) return <div>Loading...</div>
	if (error) return <div>Error: {error.message}</div>
	if (!form) return <div>No data</div>
	console.log(form);
	return (
		<EntityForm
		initialForm={form}
		initialMode={FormMode.VIEW}
		controller={controller}
		customToolbar={null}
		customSections={null}
		onAfterSave={reloadForm}
		/>
	)
}

export default ProjectFormView