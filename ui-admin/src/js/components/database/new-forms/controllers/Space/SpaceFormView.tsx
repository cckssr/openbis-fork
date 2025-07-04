import React from 'react'
import { useSpaceFormController } from '@src/js/components/database/new-forms/components/EntityFormBuilderProvider.tsx';
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import Button from '@src/js/components/common/form/Button.jsx';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.ts';
import { ProjectCreationForm } from '@src/js/components/database/new-forms/controllers/Project/ProjectCreationForm.tsx';
import AppController from '@src/js/components/AppController.js';
import pages from '@src/js/common/consts/pages.js';
import ids from '@src/js/common/consts/ids.js';

const SpaceFormView = ({ permId }: { permId: string }) => {
	const {spaceController, onEntityChange, onNewProject} = useSpaceFormController()
	const [form, setForm] = React.useState(null)
	const [loading, setLoading] = React.useState(true)
	const [error, setError] = React.useState(null)

	React.useEffect(() => {
		let mounted = true
		setLoading(true)
		spaceController.load(permId)
			.then(f => { if (mounted) setForm(f) })
			.catch(e => { if (mounted) setError(e) })
			.finally(() => { if (mounted) setLoading(false) })
		return () => { mounted = false }
	}, [spaceController, permId])

	const reloadForm = () => {
		setLoading(true);
		spaceController.load(permId)
			.then(f => setForm(f))
			.catch(e => setError(e))
			.finally(() => setLoading(false));
	};

	const handleAddProject = () => {
		AppController.getInstance().objectOpen(
			'PROJECT',
			'CREATE',
			permId
		);
	};

	const spaceToolbar = ({ form, mode, controller }: { form: Form, mode: FormMode, controller: FormController }) => (
		<>
			<Button onClick={handleAddProject}>+ Project</Button>
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
			controller={spaceController}
			customToolbar={null}
			customSections={null}
			onAfterSave={reloadForm}
			onEntityChange={onEntityChange}
			onNewProject={onNewProject}
		/>
	)
}

export default SpaceFormView