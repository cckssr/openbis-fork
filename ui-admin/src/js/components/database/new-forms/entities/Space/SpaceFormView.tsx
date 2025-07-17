import React from 'react'
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContextProvider.tsx';
import { EntityForm } from '@src/js/components/database/new-forms/components/EntityForm.tsx';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

const SpaceFormView = ({ permId, mode }: { permId: string, mode?: FormMode }) => {
	const {controller, onEntityChange, loading, error, setError, setLoading, form, setForm, reloadForm } = useEntityForm();

	console.log({form});
	if (mode === FormMode.EDIT) {
		<EntityForm
			initialForm={form}
			initialMode={FormMode.EDIT}
			controller={controller}
			customToolbar={null}
			customSections={null}
			onAfterSave={reloadForm}
		/>
	} else if (mode === FormMode.CREATE) {
		<div>Create</div>
	} else return (
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