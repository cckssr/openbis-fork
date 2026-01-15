import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { DatasetFormModel } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormModel.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { findFormFieldById, getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { createDummyDataSetIdentifierFromExperimentIdentifier, createDummySampleIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';
import { MoveService } from '@src/js/components/database/new-forms/services/MoveService.ts';

export class DatasetFormController implements IFormController {
	private openbisFacade: any;
	private deleteService: DeleteService;
	private moveService: MoveService;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
		this.deleteService = new DeleteService({ openbisFacade: this.openbisFacade });
		this.moveService = new MoveService({ openbisFacade: this.openbisFacade });
	}

	async executeOperations(operations: any[]): Promise<any> {
		const { SynchronousOperationExecutionOptions } = this.openbisFacade;
		const result = await this.openbisFacade.executeOperations(operations, new SynchronousOperationExecutionOptions());
		return result;
	}

	async load(permId: string, entityKind?: string, params?: any): Promise<Form> {
		if (entityKind === EntityKind.NEW_DATASET) {
			const typeCode = params.entityType;
			const { EntityTypePermId, DataSetTypeFetchOptions } = this.openbisFacade;
			const id = new EntityTypePermId(typeCode)
			const fetchOptions = new DataSetTypeFetchOptions()
			fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
			const types = await this.openbisFacade.getDataSetTypes([id], fetchOptions)
			const dto = types[typeCode];
			console.log('DatasetFormController.load', { dto });
			const { ExperimentPermId, SamplePermId, ExperimentFetchOptions, SampleFetchOptions } = this.openbisFacade;
			let parentDto = null;
			switch (params.parentType) {
				case EntityKind.COLLECTION:
				case EntityKind.EXPERIMENT:
					const collectionId = new ExperimentPermId(params.parentId);
					const collection = await this.openbisFacade.getExperiments([collectionId], new ExperimentFetchOptions());
					parentDto = collection[collectionId];
					params.parentId = parentDto.getIdentifier().getIdentifier();
					break;
				case EntityKind.OBJECT:
				case EntityKind.SAMPLE:
					const objectId = new SamplePermId(params.parentId);
					const object = await this.openbisFacade.getSamples([objectId], new SampleFetchOptions());
					parentDto = object[objectId];
					params.parentId = parentDto.getIdentifier().getIdentifier();
					break;
				default:
					throw new Error(`Parent type ${params.parentType} not supported`);
			}
			return DatasetFormModel.adaptNewDatasetDtoToForm(dto, permId, params);
		}
		const { DataSetPermId, DataSetFetchOptions } = this.openbisFacade;
		const id = new DataSetPermId(permId);
		const fetchOptions = new DataSetFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withSample();
		fetchOptions.withChildren();
		fetchOptions.withParents();
		fetchOptions.withProperties();
		fetchOptions.withType().withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
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
		const { DataSetCreation, EntityTypePermId, ExperimentIdentifier, SampleIdentifier } = this.openbisFacade;
		const creation = new DataSetCreation();
		creation.setCode(form.fields.find((field: any) => field.id === form.entityPermId + '-code')?.value);
		creation.setTypeId(new EntityTypePermId(form.entityType, EntityKind.DATA_SET));
		const collectionId = form.fields.find((field: any) => field.id === form.entityPermId + '-collection')?.value;
		if (collectionId) {
			creation.setExperimentId(new ExperimentIdentifier(collectionId));
		}
		const objectId = form.fields.find((field: any) => field.id === form.entityPermId + '-object')?.value;
		if (objectId) {
			creation.setSampleId(new SampleIdentifier(objectId));
		}
		getChangedEditableFieldValues(form, creation);
		const result = await this.openbisFacade.createDataSets([creation]);
		return Promise.resolve(result[0].getPermId() || '');
	}

	async _updateDataset(form: Form): Promise<number> {
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
		const result = await this.openbisFacade.updateDataSets([datasetUpdate]);
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
			}
		}

		// Finally, move the dataset itself to trashcan using DeleteService
		// Pass the permId as an object - DeleteService will extract it
		const result = await this.deleteService.moveDataSetsToTrashcan([{ permId: form.entityPermId }], deleteReason);
		if (!result.success) {
			throw new Error(result.error || 'Failed to move dataset to trashcan');
		}
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

	async move(form: Form, context?: any, params?: any): Promise<void> {
		if (!params || !params.target) {
			throw new Error('Target is required for move operation');
		}

		const result = await this.moveService.moveDataset(form.entityPermId, params.target);

		if (!result.success) {
			throw new Error(result.error || 'Failed to move dataset');
		}
		return Promise.resolve();
	}
}