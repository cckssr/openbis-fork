import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { DatasetFormModel } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormModel.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { findFormFieldById, getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { createDummyDataSetIdentifierFromExperimentIdentifier, createDummySampleIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';

export class DatasetFormController implements IFormController {
	private openbisFacade: any;
	private deleteService: DeleteService;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
		this.deleteService = new DeleteService({ openbisFacade: this.openbisFacade });
	}

	async executeOperations(operations: any[]): Promise<any> {
		const { SynchronousOperationExecutionOptions } = this.openbisFacade;
		const result = await this.openbisFacade.executeOperations(operations, new SynchronousOperationExecutionOptions());
		return result;
	}

	async load(permId: string): Promise<Form> {
		const { DataSetPermId, DataSetFetchOptions } = this.openbisFacade;
		const id = new DataSetPermId(permId);
		const fetchOptions = new DataSetFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withSample();
		fetchOptions.withChildren();
		fetchOptions.withParents();
		fetchOptions.withProperties();
		fetchOptions.withType();
		fetchOptions.withType().withPropertyAssignments();
		fetchOptions.withType().withPropertyAssignments().withPropertyType();
		fetchOptions.withModifier();
		fetchOptions.withRegistrator();
		const result = await this.openbisFacade.getDataSets([id], fetchOptions);
		const datasetDto = result[permId];
		console.log('DatasetFormController.load', { datasetDto });
		if (!datasetDto) throw new Error(`Dataset with permId ${permId} not found`);
		return DatasetFormModel.adaptDatasetDtoToForm(datasetDto);
	}

	async save(form: Form, mode: FormMode): Promise<number> {
		// TODO: implement save for dataset
		if (mode === FormMode.CREATE) {
			return this._createDataset(form);
		} else if (mode === FormMode.EDIT) {
			return this._updateDataset(form);
		} else {
			throw new Error(`Invalid form mode: ${mode}`);
		}
	}

	async _createDataset(form: Form): Promise<number> {
		console.log('DatasetFormController._createDataset', { form });
		return Promise.resolve(form.version + 1);
	}

	async _updateDataset(form: Form): Promise<number> {
		console.log('DatasetFormController._updateDataset', { form });
		const { DataSetUpdate, DataSetPermId, ExperimentIdentifier, SampleIdentifier } = this.openbisFacade;
		//const collectionIdentifier = findFormFieldById(form.fields, form.entityPermId, 'collection', true);
		//const objectIdentifier = findFormFieldById(form.fields, form.entityPermId, 'object', true);
		const datasetUpdate = new DataSetUpdate();
		datasetUpdate.setDataSetId(new DataSetPermId(form.entityPermId));
		/* if (collectionIdentifier) {
			datasetUpdate.setExperimentId(new ExperimentIdentifier(collectionIdentifier));
		}
		if (objectIdentifier) {
			datasetUpdate.setSampleId(new SampleIdentifier(objectIdentifier));
		} */
		getChangedEditableFieldValues(form, datasetUpdate);
		console.log('DatasetFormController._updateDataset', { datasetUpdate });
		const result = await this.openbisFacade.updateDataSets([datasetUpdate]);
		console.log('DatasetFormController._updateDataset', { result });
		return Promise.resolve(form.version + 1);
	}

	async checkPermissions(form: Form) {	
		return { canEdit: true, canDelete: true, canMove: true };
		/* const objId = form.entityPermId;
		const { ExperimentPermId, SampleIdentifier, DataSetPermId, RightsFetchOptions } = this.openbisFacade;
		const experimentId = new ExperimentPermId(objId);
		const collectionIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		if (typeof collectionIdentifier !== 'string' || !collectionIdentifier) {
			throw new Error('[DatasetFormController.checkPermissions] Missing collection identifier');
		}
		console.log({collectionIdentifier})
		const dummyId = new DataSetPermId(createDummyDataSetIdentifierFromExperimentIdentifier(collectionIdentifier));
		const dummyId2 = new SampleIdentifier(createDummySampleIdentifierFromSampleIdentifier(collectionIdentifier));
		console.log({dummyId, dummyId2})
		const ids = [experimentId, dummyId, dummyId2];
		console.log({ids})
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		//return new this.openbisFacade.GetRightsOperation(ids, new RightsFetchOptions());
		console.log({editable, deletable}) */
		//return { canEdit: editable, canDelete: deletable, canMove: true };
	}

	async delete(form: Form, context?: any): Promise<void> {
		console.log(`DatasetFormController.delete`, form.entityPermId, context);
		
		// If this is just a check, return early
		if (context?.checkOnly) {
			return;
		}
		
		// Get delete reason from context or use default
		const deleteReason = context?.deleteReason || 'delete via ng-ui';
		
		// Check if descendants should be deleted (from checkbox in dialog)
		const includeDescendants = context?.includeDescendants || false;
		
		// If descendants checkbox is checked, find and move descendant datasets to trashcan first using DeleteService
		if (includeDescendants) {
			const descendantDatasets = await this.getDescendantDatasets(form.entityPermId);
			if (descendantDatasets.length > 0) {
				const result = await this.deleteService.moveDataSetsToTrashcan(descendantDatasets, deleteReason);
				if (!result.success) {
					throw new Error(result.error || 'Failed to move descendant datasets to trashcan');
				}
				console.log('DatasetFormController.moved descendant datasets to trashcan:', result.count);
			}
		}
		
		// Finally, move the dataset itself to trashcan using DeleteService
		// Pass the permId as an object - DeleteService will extract it
		const result = await this.deleteService.moveDataSetsToTrashcan([{ permId: form.entityPermId }], deleteReason);
		if (!result.success) {
			throw new Error(result.error || 'Failed to move dataset to trashcan');
		}
		console.log('DatasetFormController.delete result:', result);
		return Promise.resolve();
	}
	
	/**
	 * Get descendant datasets (child datasets)
	 * @param datasetPermId The permId of the parent dataset
	 * @returns Array of descendant dataset objects
	 */
	async getDescendantDatasets(datasetPermId: string): Promise<any[]> {
		const { DataSetPermId, DataSetFetchOptions } = this.openbisFacade;
		const id = new DataSetPermId(datasetPermId);
		const fetchOptions = new DataSetFetchOptions();
		fetchOptions.withChildren && fetchOptions.withChildren();
		
		try {
			const result = await this.openbisFacade.getDataSets([id], fetchOptions);
			const dataset = result[datasetPermId];
			if (dataset && dataset.getChildren) {
				const children = dataset.getChildren();
				// Recursively get all descendants
				const allDescendants: any[] = [];
				for (const child of children) {
					allDescendants.push(child);
					const childPermId = child.getPermId ? child.getPermId() : child.permId || child;
					const childDescendants = await this.getDescendantDatasets(childPermId);
					allDescendants.push(...childDescendants);
				}
				return allDescendants;
			}
		} catch (error) {
			console.warn('DatasetFormController.getDescendantDatasets error:', error);
		}
		return [];
	}

	async getDependentEntities(form: Form): Promise<any> {
		// Check for descendant datasets (children)
		const descendantDatasets = await this.getDescendantDatasets(form.entityPermId);
		return {
			datasets: descendantDatasets,
			samples: []
		};
	}

	move(form: Form, context?: any): Promise<void> {
		console.log('DatasetFormController.move', form, context);
		return Promise.resolve();
	}
}