import React from 'react'
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContainer.tsx';
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

const SpaceFormView = ({ permId }: { permId: string }) => {
	const {controller, onEntityChange } = useEntityForm()
	const [form, setForm] = React.useState<Form | null>(null)
	const [loading, setLoading] = React.useState(true)
	const [error, setError] = React.useState<any>(null)

	React.useEffect(() => {
		let mounted = true
		setLoading(true)
		controller.load(permId)
			.then((f: Form) => { if (mounted) setForm(f) })
			.catch((e: any) => { if (mounted) setError(e) })
			.finally(() => { if (mounted) setLoading(false) })
		return () => { mounted = false }
	}, [controller, permId])

	const reloadForm = () => {
		setLoading(true);
		controller.load(permId)
			.then((f: Form) => {
				setForm(f);
				if (onEntityChange) onEntityChange(f.entityPermId, false);
			})
			.catch((e: any) => setError(e))
			.finally(() => setLoading(false));
	};


	if (loading) return <div>Loading...</div>
	if (error) return <div>Error: {error}</div>
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

export default SpaceFormView