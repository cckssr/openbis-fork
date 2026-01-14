import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyDataSetIdentifierFromSampleIdentifier, createDummySampleIdentifierFromSampleIdentifier, getSpaceCodeFromIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { EntityKind, FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { ObjectFormModel } from '@src/js/components/database/new-forms/entities/Object/ObjectFormModel.ts';
import { getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';
import { MoveService } from '@src/js/components/database/new-forms/services/MoveService.ts';

export class ObjectFormController implements IFormController {
	private openbisFacade: any;
	private deleteService: DeleteService;
	private moveService: MoveService;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
		this.deleteService = new DeleteService({ openbisFacade: this.openbisFacade });
		this.moveService = new MoveService({ openbisFacade: this.openbisFacade });
	}

	async _getNextSequenceForType(sampleTypeCode: string): Promise<string> {
		const { SampleSearchCriteria, SampleFetchOptions } = this.openbisFacade;
		const criteria = new SampleSearchCriteria();
		criteria.withType().withCode().thatEquals(sampleTypeCode);
		criteria.withCode().thatStartsWith(sampleTypeCode);
		const fetchOptions = new SampleFetchOptions();

		const result = await this.openbisFacade.searchSamples(criteria, fetchOptions);
		const samples = result.getObjects();
		const nextNumber = Math.max(...samples.map((s: any) => parseInt(s.getCode().match(/(\d+)$/)?.[1] ?? '0', 10))) + 1;
		return sampleTypeCode + nextNumber;
	}

	async load(permId: string, entityKind?: string, params?: any): Promise<Form> {
		if (entityKind === EntityKind.NEW_OBJECT) {
			const typeCode = params.entityType;
			params.defaultCode = await this._getNextSequenceForType(typeCode);
			const { EntityTypePermId, SampleTypeFetchOptions } = this.openbisFacade;
			const id = new EntityTypePermId(typeCode)
			const fetchOptions = new SampleTypeFetchOptions()
			fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
			const types = await this.openbisFacade.getSampleTypes([id], fetchOptions)
			const dto = types[typeCode];
			return ObjectFormModel.adaptNewObjectDtoToForm(dto, permId, params);
		}
		const { SampleSearchCriteria, SampleFetchOptions } = this.openbisFacade;
		const criteria = new SampleSearchCriteria();
		criteria.withPermId().thatEquals(permId);
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withProperties();
		fetchOptions.withType().withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		fetchOptions.withExperiment();
		fetchOptions.withParents();
		fetchOptions.withDataSets();
		fetchOptions.withModifier();
		fetchOptions.withRegistrator();
		const result = await this.openbisFacade.searchSamples(criteria, fetchOptions);
		const sampleDto = Object.values(result.objects)[0];
		console.log('ObjectFormController.load', { sampleDto });
		if (!sampleDto) throw new Error(`Sample with permId ${permId} not found`);
		return ObjectFormModel.adaptSampleDtoToForm(sampleDto);
	}

	async save(form: Form, mode: FormMode): Promise<any> {
		if (mode === FormMode.CREATE) {
			return this._createObject(form);
		} else if (mode === FormMode.EDIT) {
			return this._updateObject(form);
		} else {
			throw new Error(`Invalid form mode: ${mode}`);
		}
	}


	async _createObject(form: Form): Promise<any> {
		const sampleCreation = this._createSample(form);
		const result = await this.openbisFacade.createSamples([sampleCreation]);
		return Promise.resolve(result[0].getPermId());
	}

	async _updateObject(form: Form): Promise<any> {
		const sampleUpdate = this._updateSample(form);
		const result = await this.openbisFacade.updateSamples([sampleUpdate]);
		return Promise.resolve(form.version ? form.version + 1 : 1);
	}

	_createSampleCreation(parameters: any): Promise<any> {
		const { SampleCreation, EntityTypePermId } = this.openbisFacade;
		const creation = new SampleCreation();
		//setBasics(creation, parameters);
		creation.setTypeId(new EntityTypePermId(parameters["sampleType"], EntityKind.SAMPLE));
		const sampleCode = parameters["sampleCode"]
		if (sampleCode) {
			creation.setCode(sampleCode);
		}
		return creation;
	}

	_createSample(form: Form): Promise<any> {
		const { SampleCreation, EntityTypePermId, ProjectIdentifier, SpacePermId } = this.openbisFacade;
		const creation = new SampleCreation();
		creation.setCode(form.fields.find((field: any) => field.id === form.entityPermId + '-code')?.value);
		creation.setTypeId(new EntityTypePermId(form.entityType, EntityKind.SAMPLE.toUpperCase()));
		const projectId = form.fields.find((field: any) => field.id === form.entityPermId + '-project')?.value;
		if (projectId) {
			creation.setProjectId(new ProjectIdentifier(projectId));
		}
		creation.setSpaceId(new SpacePermId(getSpaceCodeFromIdentifier(projectId || '')));

		getChangedEditableFieldValues(form, creation);
		return creation;
	}

	_updateSample(form: Form): Promise<any> {
		const { SampleUpdate, SamplePermId } = this.openbisFacade;
		const update = new SampleUpdate();
		update.setSampleId(new SamplePermId(form.entityPermId));
		getChangedEditableFieldValues(form, update);
		return update;
	}

	async checkPermissions(form: Form) {
		const { SamplePermId, DataSetPermId, SampleIdentifier } = this.openbisFacade;
		const objId = form.entityPermId;
		const samplePermId = new this.openbisFacade.SamplePermId(objId);
		const sampleIdentifierValue = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		if (typeof sampleIdentifierValue !== 'string' || !sampleIdentifierValue) {
			throw new Error('[ObjectFormController.checkPermissions] Missing sample identifier');
		}
		const sampleIdentifier = sampleIdentifierValue;
		const dummyId = new DataSetPermId(createDummyDataSetIdentifierFromSampleIdentifier(sampleIdentifier));
		const dummyId2 = new SampleIdentifier(createDummySampleIdentifierFromSampleIdentifier(sampleIdentifier));
		const ids = [samplePermId, dummyId, dummyId2];
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
		
		// Check if descendants should be deleted (from checkbox in dialog)
		const includeDescendants = context?.includeDescendants || false;
		
		// Move all datasets to trashcan first using DeleteService
		if (dependentEntities.datasets && dependentEntities.datasets.length > 0) {
			const result = await this.deleteService.moveDataSetsToTrashcan(dependentEntities.datasets, deleteReason);
			if (!result.success) {
				throw new Error(result.error || 'Failed to move datasets to trashcan');
			}
		}
		// If descendants checkbox is checked, move all descendant objects and their datasets to trashcan
		// Handle both raw structure (children) and normalized structure (samples)
		const children = dependentEntities.children || dependentEntities.samples || [];
		if (includeDescendants && children.length > 0) {
			await this.deleteDescendantObjects(children, deleteReason);
		}
		
		// Finally, move the object (sample) itself to trashcan using DeleteService
		const sampleIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
		if (!sampleIdentifier || typeof sampleIdentifier !== 'string') {
			throw new Error('Sample identifier not found');
		}
		const result = await this.deleteService.moveSamplesToTrashcan(
			[{ identifier: sampleIdentifier }],
			deleteReason
		);
		if (!result.success) {
			throw new Error(result.error || 'Failed to move object to trashcan');
		}
		return Promise.resolve();
	}
	
	/**
	 * Recursively delete descendant objects and their datasets
	 * @param children Array of child sample objects
	 * @param reason Deletion reason
	 */
	async deleteDescendantObjects(children: any[], reason: string): Promise<void> {
		if (!children || children.length === 0) {
			return;
		}
		
		// Process each child
		for (const child of children) {
			// Get child's datasets and children
			const { SamplePermId, SampleFetchOptions } = this.openbisFacade;
			const childPermId = child.getPermId ? child.getPermId() : child.permId || child;
			const id = new SamplePermId(childPermId);
			const fetchOptions = new SampleFetchOptions();
			fetchOptions.withDataSets && fetchOptions.withDataSets();
			fetchOptions.withChildren && fetchOptions.withChildren();
			const result = await this.openbisFacade.getSamples([id], fetchOptions);
			const childSample = result[childPermId];
			
			if (childSample) {
				const childDatasets = childSample.getDataSets ? childSample.getDataSets() : [];
				const childChildren = childSample.getChildren ? childSample.getChildren() : [];
				
				// Move child's datasets to trashcan using DeleteService
				if (childDatasets.length > 0) {
					const result = await this.deleteService.moveDataSetsToTrashcan(childDatasets, reason);
					if (!result.success) {
						throw new Error(result.error || 'Failed to move child datasets to trashcan');
					}
				}
				
				// Recursively delete grandchildren
				if (childChildren.length > 0) {
					await this.deleteDescendantObjects(childChildren, reason);
				}
				
				// Move child object to trashcan using DeleteService
				const childIdentifier = child.getIdentifier ? child.getIdentifier().getIdentifier() : child.identifier || child;
				const result = await this.deleteService.moveSamplesToTrashcan(
					[{ identifier: childIdentifier }],
					reason
				);
				if (!result.success) {
					throw new Error(result.error || 'Failed to move child object to trashcan');
				}
			}
		}
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

	async move(form: Form, context?: any, params?: any): Promise<void> {
		if (!params || !params.target) {
			throw new Error('Target is required for move operation');
		}

		const includeDescendants = params.moveDescendants || false;
		const result = await this.moveService.moveObject(form.entityPermId, params.target, includeDescendants);

		if (!result.success) {
			throw new Error(result.error || 'Failed to move object');
		}
		return Promise.resolve();
	}
}