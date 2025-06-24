import React from 'react'
import { useProjectFormController } from '@src/js/components/database/new-forms/components/EntityFormBuilderContext.tsx'
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx'
import { FormMode } from '@src/js/components/database/new-forms/types/form.types.ts'
import Button from '@src/js/components/common/form/Button.jsx'

const ProjectFormView = ({ permId }) => {
	const controller = useProjectFormController()
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

	const projectToolbar = ({ form, mode, controller }) => (
		<>
			<Button>+ Project</Button>
			<Button>Edit</Button>
			<Button>More...</Button>
			{/* ...other space-specific actions... */}
		</>
	);

	const projectSections = ({ form }) => (
		<>
			<div title="Identification Info">
				{/* Render code, registrator, registration date */}
			</div>
			<div title="General">
				{/* Render description, etc. */}
			</div>
		</>
	);


	if (loading) return <div>Loading...</div>
	if (error) return <div>Error: {error.message}</div>
	if (!form) return <div>No data</div>
	console.log(form);
	return (
		<EntityForm
			initialForm={form}
			initialMode={FormMode.VIEW}
			controller={controller}
			renderToolbar={null}
			renderSections={null}
		/>
	)
}

export default ProjectFormView