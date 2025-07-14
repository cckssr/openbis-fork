import { findFormFieldById, Form, FormFieldDataType, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/entities/FormController.ts';
import { adaptDatasetDtoToForm } from '@src/js/components/database/new-forms/adapters/entity.adapter.ts';
import { fetchRights } from '@src/js/components/database/new-forms/entities/AuthorizationService.ts';
import { getProjectCodeFromExperimentIdentifier, getProjectIdentifierFromExperimentIdentifier, guid } from '@src/js/components/database/new-forms/utils/Utils';
import { openbis } from '@srcV3/openbis.esm';

export class DatasetFormController implements FormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
		const { DataSetPermId, DataSetFetchOptions } = this.openbisFacade;
		const id = new DataSetPermId(permId);
		const fetchOptions = new DataSetFetchOptions()
        fetchOptions.withExperiment()
        fetchOptions.withSample()
        fetchOptions.withParents()
        fetchOptions.withProperties()
		fetchOptions.withType()
        const result = await this.openbisFacade.getDataSets([id], fetchOptions);
		console.log({result})
		const datasetDto = result[permId];

		if (!datasetDto) throw new Error(`Dataset with permId ${permId} not found`);
		return adaptDatasetDtoToForm(datasetDto);
	}

	async save(form: Form): Promise<number> {
		console.log('--- CONTROLLER: SAVING FORM ---');
		console.log(JSON.stringify(form, null, 2));
		console.log('-----------------------------');
		// Simulate a successful save by returning an incremented version number.
		return Promise.resolve(form.version + 1);
	}

	async checkPermissions(form: Form) {
		/* const objId = form.entityPermId;
		const { ExperimentPermId, SampleIdentifier, DataSetPermId } = this.openbisFacade;
		const experimentId = new ExperimentPermId(objId);
		const collectionIdentifier = findFormFieldById(form.fields, 'identifier')?.value;
		console.log({collectionIdentifier})
		const dummyId = new DataSetPermId(getProjectIdentifierFromExperimentIdentifier(collectionIdentifier) + "/DUMMY_" + guid());
        const dummyId2 = new SampleIdentifier(getProjectIdentifierFromExperimentIdentifier(collectionIdentifier) + "/DUMMY2_" + guid());
		const ids = [experimentId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		console.log({editable, deletable})
		return { canEdit: editable, canDelete: deletable, canMove: true }; */
		return { canEdit: true, canDelete: true, canMove: true };
	}

	edit(form: Form): void {
		console.log(`CONTROLLER: Switching to Edit Mode for ${form.entityPermId}`);
	}

	delete(form: Form): void {
		console.log(`CONTROLLER: Deleting ${form.entityPermId}`);
	}

	move(form: Form): void {
		console.log(`CONTROLLER: Moving ${form.entityPermId}`);
	}
}