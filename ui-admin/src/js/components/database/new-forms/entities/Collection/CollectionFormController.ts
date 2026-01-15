import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { CollectionFormModel } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormModel.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyDataSetIdentifierFromExperimentIdentifier, createDummySampleIdentifierFromSampleIdentifier, getProjectIdentifierFromExperimentIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById, getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';
import { MoveService } from '@src/js/components/database/new-forms/services/MoveService.ts';

export class CollectionFormController implements IFormController {
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
		if (entityKind === EntityKind.NEW_COLLECTION) {
			const { EntityTypePermId, ExperimentTypeFetchOptions } = this.openbisFacade;
			const typeCode = params.entityType;
			const id = new EntityTypePermId(typeCode)
			const fetchOptions = new ExperimentTypeFetchOptions()
			fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
			const types = await this.openbisFacade.getExperimentTypes([id], fetchOptions)
			const { ProjectPermId, ProjectFetchOptions } = this.openbisFacade;
			const projectId = new ProjectPermId(params.parentId);
			const project = await this.openbisFacade.getProjects([projectId], new ProjectFetchOptions());
			const projectDto = project[projectId];
			params.parentId = projectDto.getIdentifier().getIdentifier();
			return CollectionFormModel.adaptNewCollectionDtoToForm(permId, types[typeCode], params);
		}
		const { ExperimentPermId, ExperimentFetchOptions } = this.openbisFacade;
		const id = new ExperimentPermId(permId);
		const fetchOptions = new ExperimentFetchOptions();
		fetchOptions.withProperties()
		fetchOptions.withDataSets().withProperties()
		fetchOptions.withType().withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
		fetchOptions.withProject();
		fetchOptions.withDataSets();
		fetchOptions.withModifier();
		fetchOptions.withRegistrator();

		// Add more fetch options as needed
		const result = await this.openbisFacade.getExperiments([id], fetchOptions);

		const collectionDto = result[permId];

		if (!collectionDto) throw new Error(`[CollectionFormController.load] Collection with permId ${permId} not found`);
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
		const { ExperimentCreation, EntityTypePermId, ProjectIdentifier } = this.openbisFacade;
		const experimentCreation = new ExperimentCreation();
		experimentCreation.setTypeId(new EntityTypePermId(form.entityType));
		experimentCreation.setProjectId(new ProjectIdentifier(findFormFieldById(form.fields, form.entityPermId, 'project', true) as string));
		experimentCreation.setCode(findFormFieldById(form.fields, form.entityPermId, 'code', true) as string);
		const result = await this.openbisFacade.createExperiments([experimentCreation]);
		return result[0].getPermId();
	}

	async _updateCollection(form: Form): Promise<number> {
		const { ExperimentUpdate, ExperimentPermId, ProjectIdentifier } = this.openbisFacade;
		const experimentUpdate = new ExperimentUpdate();
		experimentUpdate.setExperimentId(new ExperimentPermId(form.entityPermId));
		experimentUpdate.setProjectId(new ProjectIdentifier(getProjectIdentifierFromExperimentIdentifier(findFormFieldById(form.fields, form.entityPermId, 'identifier', true) as string)));

		getChangedEditableFieldValues(form, experimentUpdate);

		const result = await this.openbisFacade.updateExperiments([experimentUpdate]);
		return Promise.resolve(form.version + 1);
	}

	async checkPermissions(form: Form) {
		const objId = form.entityPermId;
		const { ExperimentPermId, SampleIdentifier, DataSetPermId } = this.openbisFacade;
		const experimentId = new ExperimentPermId(objId);
		const collectionIdentifierValue = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		if (typeof collectionIdentifierValue !== 'string' || !collectionIdentifierValue) {
			throw new Error('[CollectionFormController.checkPermissions] Missing collection identifier');
		}
		const collectionIdentifier = collectionIdentifierValue;
		const dummyId = new DataSetPermId(createDummyDataSetIdentifierFromExperimentIdentifier(collectionIdentifier));
		const dummyId2 = new SampleIdentifier(createDummySampleIdentifierFromSampleIdentifier(collectionIdentifier));
		const ids = [experimentId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		return { canEdit: editable, canDelete: deletable, canMove: true };
		//return { canEdit: true, canDelete: true, canMove: true };
	}

	async delete(form: Form, context?: any): Promise<void> {

		// If this is just a check, return early
		if (context?.checkOnly) {
			return;
		}

		// Get dependent entities if not provided in context
		// Use rawDependentEntities from context if available (from normalized structure)
		let dependentEntities = context?.rawDependentEntities || context?.dependentEntities;
		if (!dependentEntities) {
			dependentEntities = await this.getDependentEntities(form);
		}

		// Get delete reason from context or use default
		const deleteReason = context?.deleteReason || 'delete via ng-ui';

		// Move all objects (samples) to trashcan first using DeleteService
		if (dependentEntities.samples && dependentEntities.samples.length > 0) {
			const result = await this.deleteService.moveSamplesToTrashcan(dependentEntities.samples, deleteReason);
			if (!result.success) {
				throw new Error(result.error || 'Failed to move samples to trashcan');
			}
		}

		// Move all datasets to trashcan using DeleteService
		if (dependentEntities.datasets && dependentEntities.datasets.length > 0) {
			const result = await this.deleteService.moveDataSetsToTrashcan(dependentEntities.datasets, deleteReason);
			if (!result.success) {
				throw new Error(result.error || 'Failed to move datasets to trashcan');
			}
		}

		// Finally, move the collection (experiment) itself to trashcan using DeleteService
		const collectionIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		if (!collectionIdentifier || typeof collectionIdentifier !== 'string') {
			throw new Error('Collection identifier not found');
		}
		// Pass the identifier string directly - DeleteService will create the ExperimentIdentifier
		const result = await this.deleteService.moveExperimentsToTrashcan([{ identifier: collectionIdentifier }], deleteReason);
		if (!result.success) {
			throw new Error(result.error || 'Failed to move collection to trashcan');
		}
		return Promise.resolve();
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

	async move(form: Form, context?: any, params?: any): Promise<void> {

		if (!params || !params.target) {
			throw new Error('Target is required for move operation');
		}

		// Target should be a Project entity
		const targetType = params.target['@type'];
		if (targetType !== 'as.dto.project.Project') {
			throw new Error(`Invalid target type for collection move: ${targetType}. Expected Project.`);
		}

		const result = await this.moveService.moveCollection(form.entityPermId, params.target);

		if (!result.success) {
			throw new Error(result.error || 'Failed to move collection');
		}

		return Promise.resolve();
	}
}