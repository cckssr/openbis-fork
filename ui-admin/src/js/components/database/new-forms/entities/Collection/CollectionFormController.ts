import { Form, FormFieldDataType, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/entities/FormController.ts';
import { adaptCollectionDtoToForm } from '@src/js/components/database/new-forms/adapters/entity.adapter.ts';
import { fetchRights } from '@src/js/components/database/new-forms/entities/AuthorizationService.ts';
import { createDummyDataSetIdentifierFromExperimentIdentifier, createDummySampleIdentifierFromSampleIdentifier, getProjectCodeFromExperimentIdentifier, getProjectIdentifierFromExperimentIdentifier, guid } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';

export class CollectionFormController implements FormController {
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
		fetchOptions.withProject();
		fetchOptions.withDataSets();
		// Add more fetch options as needed
		const result = await this.openbisFacade.getExperiments([id], fetchOptions);

		const collectionDto = result[permId];

		//const roles = await getUserRole(this.openbisFacade, this.user==='admin', permId);
		//console.log({roles});
		console.log(collectionDto);
		if (!collectionDto) throw new Error(`Collection with permId ${permId} not found`);
		return adaptCollectionDtoToForm(collectionDto);
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
		const collectionIdentifier = findFormFieldById(form.fields, 'identifier')?.value;
		console.log({collectionIdentifier})
		const dummyId = new DataSetPermId(createDummyDataSetIdentifierFromExperimentIdentifier(collectionIdentifier));
        const dummyId2 = new SampleIdentifier(createDummySampleIdentifierFromSampleIdentifier(collectionIdentifier));
		const ids = [experimentId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		console.log({editable, deletable})
		return { canEdit: editable, canDelete: deletable, canMove: true };
		//return { canEdit: true, canDelete: true, canMove: true };
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