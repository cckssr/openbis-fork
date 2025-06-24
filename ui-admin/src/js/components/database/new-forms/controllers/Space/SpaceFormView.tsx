import React from 'react'
import { useSpaceFormController } from '@src/js/components/database/new-forms/components/EntityFormBuilderContext.tsx';
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import Button from '@src/js/components/common/form/Button.jsx';
import { Grid2 } from '@mui/material';
import { FormFieldRenderer } from '@src/js/components/database/new-forms/components/FormFieldRenderer.tsx';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.ts';

const SpaceFormView = ({ permId }: { permId: string }) => {
	const controller = useSpaceFormController()
	const [form, setForm] = React.useState(null)
	const [loading, setLoading] = React.useState(true)
	const [error, setError] = React.useState(null)

	React.useEffect(() => {
		let mounted = true
		setLoading(true)
		controller.load(permId)
			.then(f => { if (mounted) setForm(f) })
			.catch(e => { if (mounted) setError(e) })
			.finally(() => { if (mounted) setLoading(false) })
		return () => { mounted = false }
	}, [controller, permId])

	const spaceToolbar = ({ form, mode, controller }: { form: Form, mode: FormMode, controller: FormController }) => (
		<>
			<Button>+ Project</Button>
			<Button>Edit</Button>
			<Button>More...</Button>
			{/* ...other space-specific actions... */}
		</>
	);

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
		/>
	)
}

export default SpaceFormView