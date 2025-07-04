import React from 'react'
import { useProjectFormController } from '@src/js/components/database/new-forms/components/EntityFormBuilderProvider.tsx'
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx'
import { FormMode } from '@src/js/components/database/new-forms/types/form.types.ts'
import Button from '@src/js/components/common/form/Button.jsx'

const ProjectFormView = ({ permId }) => {
	const {projectController} = useProjectFormController()
	const [form, setForm] = React.useState(null)
	const [loading, setLoading] = React.useState(true)
	const [error, setError] = React.useState(null)

	React.useEffect(() => {
		let mounted = true
		setLoading(true)
		projectController.load(permId)
			.then(f => { if (mounted) setForm(f) })
			.catch(e => { if (mounted) setError(e) })
			.finally(() => { if (mounted) setLoading(false) })
		return () => { mounted = false }
	}, [projectController, permId])

	const projectToolbar = ({ form, mode, controller }) => (
		<>
			<Button>+ Project</Button>
			<Button>Edit</Button>
			<Button>More...</Button>
			{/* ...other space-specific actions... */}
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
			controller={projectController}
			customToolbar={null}
			customSections={null}
		/>
	)
}

export default ProjectFormView