import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController';
import { adaptSampleDtoToForm } from '@src/js/components/database/new-forms/entities/Object/ObjectAdapter.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/AuthorizationService.ts';
import { createDummyDataSetIdentifierFromSampleIdentifier, createDummySampleIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';

export class ObjectFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
		const { SampleSearchCriteria, SampleFetchOptions } = this.openbisFacade;
		const criteria = new SampleSearchCriteria();
		criteria.withPermId().thatEquals(permId);
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withProperties();
		fetchOptions.withType();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		fetchOptions.withExperiment();
		fetchOptions.withParents();
		fetchOptions.withDataSets();

		const result = await this.openbisFacade.searchSamples(criteria, fetchOptions);
		const sampleDto = Object.values(result.objects)[0];
		
		if (!sampleDto) throw new Error(`Sample with permId ${permId} not found`);
		return adaptSampleDtoToForm(sampleDto);
	}

	async save(form: Form): Promise<number> {
		console.log('--- CONTROLLER: SAVING FORM ---');
		console.log(JSON.stringify(form, null, 2));
		console.log('-----------------------------');
		// Simulate a successful save by returning an incremented version number.
		return Promise.resolve(form.version + 1);
	}

	async checkPermissions(form: Form) {
		const { SamplePermId, DataSetPermId, SampleIdentifier } = this.openbisFacade;
		const objId = form.entityPermId;
		const samplePermId = new this.openbisFacade.SamplePermId(objId);
		const sampleIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		const dummyId = new DataSetPermId(createDummyDataSetIdentifierFromSampleIdentifier(sampleIdentifier));
        const dummyId2 = new SampleIdentifier(createDummySampleIdentifierFromSampleIdentifier(sampleIdentifier));
		const ids = [samplePermId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		console.log({editable, deletable})
		return { canEdit: editable, canDelete: deletable, canMove: true };
		//return { canEdit: true, canDelete: true, canMove: true };
	}

	async delete(form: Form, context?: any): Promise<void> {
		console.log(`CONTROLLER: Deleting ${form.entityPermId}`, context);
	}

	async getDependentEntities(form: Form): Promise<any> {
		// For samples, check for datasets and child samples
		const { SamplePermId, SampleFetchOptions } = this.openbisFacade;
		const id = new SamplePermId(form.entityPermId);
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withDataSets && fetchOptions.withDataSets();
		fetchOptions.withChildren && fetchOptions.withChildren();
		const result = await this.openbisFacade.getSamples([id], fetchOptions);
		const sample = result[form.entityPermId];
		
		return { 
			datasets: sample.getDataSets ? sample.getDataSets() : [], 
			children: sample.getChildren ? sample.getChildren() : [] 
		};
	}

	move(form: Form): void {
		console.log(`CONTROLLER: Moving ${form.entityPermId}`);
	}
}