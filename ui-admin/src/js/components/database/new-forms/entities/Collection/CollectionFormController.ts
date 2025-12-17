import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { CollectionFormModel } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormModel.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyDataSetIdentifierFromExperimentIdentifier, createDummySampleIdentifierFromSampleIdentifier, getProjectIdentifierFromExperimentIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById, getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

export class CollectionFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string, entityKind?: string, params?: any, type?: string): Promise<Form> {
		console.log('CollectionFormController.load', { permId, entityKind, params, type });
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
		fetchOptions.withModifier();
		fetchOptions.withRegistrator();

		// Add more fetch options as needed
		const result = await this.openbisFacade.getExperiments([id], fetchOptions);

		const collectionDto = result[permId];

		//const roles = await getUserRole(this.openbisFacade, this.user==='admin', permId);
		//console.log({roles});
		console.log('collectionDto: ', collectionDto);
		if (!collectionDto) throw new Error(`Collection with permId ${permId} not found`);
		return CollectionFormModel.adaptCollectionDtoToForm(collectionDto);
	}

	async save(form: Form, mode: FormMode): Promise<number> {
		if (mode === FormMode.CREATE) {
			return this._createCollection(form);
		  } else if (mode === FormMode.EDIT) {
			return this._updateCollection(form);
		  } else {
			throw new Error(`Invalid form mode: ${mode}`);
		  }
	}

	async _createCollection(form: Form): Promise<number> {
		console.log('CollectionFormController._createCollection', { form });
		return Promise.resolve(form.version + 1);
	}

	async _updateCollection(form: Form): Promise<number> {
		const { ExperimentUpdate, ExperimentPermId, ProjectIdentifier } = this.openbisFacade;
		const experimentUpdate = new ExperimentUpdate();
		experimentUpdate.setExperimentId(new ExperimentPermId(form.entityPermId));
		experimentUpdate.setProjectId(new ProjectIdentifier(getProjectIdentifierFromExperimentIdentifier(findFormFieldById(form.fields, form.entityPermId, 'identifier', true) as string)));
		
		const properties = getChangedEditableFieldValues(form);
		experimentUpdate.setProperties(properties);
		
		const result = await this.openbisFacade.updateExperiments([ experimentUpdate ]);
		console.log('CollectionFormController._updateCollection', { result });
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