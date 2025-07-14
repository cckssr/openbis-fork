import React from 'react'
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContainer.tsx'
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx'
import { FormAction, FormMode, Form } from '@src/js/components/database/new-forms/types/form.types.ts'

const CollectionFormView = ({ permId }: { permId: string }) => {
	const {controller} = useEntityForm()
	const [form, setForm] = React.useState<Form | null>(null)
	const [loading, setLoading] = React.useState(true)
	const [error, setError] = React.useState<any>(null)

	React.useEffect(() => {
		let mounted = true
		setLoading(true)	
		controller.load(permId)
			.then((f: Form) => { if (mounted) setForm(f) })
			.catch((e: any) => { if (mounted) setError(e) })
			.finally(() => { if (mounted) setLoading(false) })/*  */
		return () => { mounted = false }
	}, [controller, permId])

	const setFormMode = (mode: FormMode) => {
		if (form) {
			setForm(prevForm => ({ ...prevForm, mode }) as Form);
		}
	}

	const handleEdit = () => {
		console.log('Edit action');
		setFormMode(FormMode.EDIT);
		console.log(form);
	}

	const handleCreateDataset = () => {
		console.log('Create Dataset action');
	}
	
	const actions: FormAction[] = [
		{
			name: 'update',
			label: 'Update',
			component: 'button',
			handler: () => { console.log('Update action'); },
			isAllowed: true,
			isVisible: true
		},
		{
			name: 'create-dataset',
			label: 'Create Dataset',
			component: 'button',
			handler: handleCreateDataset,
			isAllowed: true,
			isVisible: true
		},
	]


	if (loading) return <div>Loading...</div>
	if (error) return <div>Error: {error.message}</div>
	if (!form) return <div>No data</div>
	if (form) form.actions = actions;
	console.log(form);
	return (
		<EntityForm
			initialForm={form}
			initialMode={form.mode || FormMode.VIEW}
			controller={controller}
			customToolbar={null}
			customSections={null}
		/>
	)
}

export default CollectionFormView