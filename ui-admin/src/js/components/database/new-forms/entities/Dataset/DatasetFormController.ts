import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { adaptDatasetDtoToForm } from '@src/js/components/database/new-forms/entities/Dataset/DatasetAdapter.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';

export class DatasetFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
		const { DataSetPermId, DataSetFetchOptions } = this.openbisFacade;
		const id = new DataSetPermId(permId);
		const fetchOptions = new DataSetFetchOptions();
        fetchOptions.withExperiment();
        fetchOptions.withSample();
        fetchOptions.withParents();
        fetchOptions.withProperties();
		fetchOptions.withType();
		fetchOptions.withType().withPropertyAssignments();
		fetchOptions.withType().withPropertyAssignments().withPropertyType();
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

	async delete(form: Form, context?: any): Promise<void> {
		console.log(`CONTROLLER: Deleting ${form.entityPermId}`, context);
	}

	async getDependentEntities(form: Form): Promise<any> {
		// Datasets typically don't have dependent entities
		return { 
			datasets: [], 
			samples: [] 
		};
	}

	move(form: Form, context?: any): Promise<void> {
		console.log('DatasetFormController.move', form, context);
		return Promise.resolve();
	}
}