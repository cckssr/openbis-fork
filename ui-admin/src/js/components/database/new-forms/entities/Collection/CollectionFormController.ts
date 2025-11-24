import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { CollectionFormModel } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormModel.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyDataSetIdentifierFromExperimentIdentifier, createDummySampleIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';

export class CollectionFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
		const { ExperimentPermId, ExperimentFetchOptions } = this.openbisFacade;
		const id = new ExperimentPermId(permId);
		const fetchOptions = new ExperimentFetchOptions();
		fetchOptions.withProperties()
		fetchOptions.withDataSets().withProperties()
		fetchOptions.withType();
		fetchOptions.withType().withPropertyAssignments();
		fetchOptions.withType().withPropertyAssignments().withPropertyType();
		fetchOptions.withProject();
		fetchOptions.withDataSets();
		// Add more fetch options as needed
		const result = await this.openbisFacade.getExperiments([id], fetchOptions);

		const collectionDto = result[permId];

		//const roles = await getUserRole(this.openbisFacade, this.user==='admin', permId);
		//console.log({roles});
		console.log(collectionDto);
		if (!collectionDto) throw new Error(`Collection with permId ${permId} not found`);
		return CollectionFormModel.adaptCollectionDtoToForm(collectionDto);
	}

	async save(form: Form): Promise<number> {
		console.log('--- CONTROLLER: SAVING FORM ---');
		console.log(JSON.stringify(form, null, 2));
		console.log('-----------------------------');
		// Simulate a successful save by returning an incremented version number.
		return Promise.resolve(form.version + 1);
	}

	async checkPermissions(form: Form) {
		const objId = form.entityPermId;
		const { ExperimentPermId, SampleIdentifier, DataSetPermId } = this.openbisFacade;
		const experimentId = new ExperimentPermId(objId);
		const collectionIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		console.log({collectionIdentifier})
		const dummyId = new DataSetPermId(createDummyDataSetIdentifierFromExperimentIdentifier(collectionIdentifier));
        const dummyId2 = new SampleIdentifier(createDummySampleIdentifierFromSampleIdentifier(collectionIdentifier));
		const ids = [experimentId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		console.log({editable, deletable})
		return { canEdit: editable, canDelete: deletable, canMove: true };
		//return { canEdit: true, canDelete: true, canMove: true };
	}

	async delete(form: Form, context?: any): Promise<void> {
		console.log(`CONTROLLER: Deleting ${form.entityPermId}`, context);
	}

	async getDependentEntities(form: Form): Promise<any> {
		// For experiments, check for samples and datasets
		const { ExperimentPermId, ExperimentFetchOptions } = this.openbisFacade;
		const id = new ExperimentPermId(form.entityPermId);
		const fetchOptions = new ExperimentFetchOptions();
		fetchOptions.withSamples && fetchOptions.withSamples();
		fetchOptions.withDataSets && fetchOptions.withDataSets();
		const result = await this.openbisFacade.getExperiments([id], fetchOptions);
		const experiment = result[form.entityPermId];
		
		return { 
			samples: experiment.getSamples ? experiment.getSamples() : [], 
			datasets: experiment.getDataSets ? experiment.getDataSets() : [] 
		};
	}

	move(form: Form, context?: any): Promise<void> {
		console.log('CollectionFormController.move', form, context);
		return Promise.resolve();
	}
}