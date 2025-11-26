import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyDataSetIdentifierFromSampleIdentifier, createDummySampleIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { EntityKind, FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { ObjectFormModel } from '@src/js/components/database/new-forms/entities/Object/ObjectFormModel.ts';
import { getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';

export class ObjectFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
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
		console.log({ nextNumber })
		return sampleTypeCode + nextNumber;
	}

	async load(permId: string, entityKind?: string, params?: any, type?: string): Promise<Form> {
		console.log('ObjectFormController.load', { permId, type, entityKind, params });
		if (entityKind === EntityKind.NEW_OBJECT) {
			params.defaultCode = await this._getNextSequenceForType('ENTRY');
			return ObjectFormModel.adaptNewEntryDtoToForm(type || '', permId, params);
			//return ObjectFormModel.adaptNewDefaultObjectDtoToForm(type || '', permId, params);
		}
		const { SampleSearchCriteria, SampleFetchOptions } = this.openbisFacade;
		const criteria = new SampleSearchCriteria();
		criteria.withPermId().thatEquals(permId);
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withProperties();
		fetchOptions.withType();
		fetchOptions.withType().withPropertyAssignments();
		fetchOptions.withType().withPropertyAssignments().withPropertyType();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		fetchOptions.withExperiment();
		fetchOptions.withParents();
		fetchOptions.withDataSets();

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
		console.log('ObjectFormController._createObject', { form });
		const sampleCreation = this._createSample(form);
		const result = await this.openbisFacade.createSamples([sampleCreation]);
		console.log('ObjectFormController._createObject', { result });
		return Promise.resolve(result[0].getPermId());
	}

	async _updateObject(form: Form): Promise<any> {
		console.log('ObjectFormController._updateObject', { form });
		const sampleUpdate = this._updateSample(form);
		console.log('ObjectFormController._updateObject', { sampleUpdate });
		const result = await this.openbisFacade.updateSamples([sampleUpdate]);
		console.log('ObjectFormController._updateObject', result);
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
		console.log('ObjectFormController._createSample', { form });
		const { SampleCreation, EntityTypePermId, ExperimentPermId, SpacePermId } = this.openbisFacade;
		const creation = new SampleCreation();
		creation.setTypeId(new EntityTypePermId(form.entityType, EntityKind.SAMPLE));
		creation.setExperimentId(new ExperimentPermId(form.meta.experimentPermId));
		creation.setSpaceId(new SpacePermId(form.meta.spacePermId));
		return creation;
	}

	_updateSample(form: Form): Promise<any> {
		console.log('ObjectFormController._updateSample', { form });
		const { SampleUpdate, SamplePermId } = this.openbisFacade;
		const update = new SampleUpdate();
		update.setSampleId(new SamplePermId(form.entityPermId));
		/* const metadata = update.getMetaData();
		form.fields
		.filter(field => !field.readOnly)
		.filter(field => field.dataType === FormFieldDataType.WORD_PROCESSOR_CLASSIC 
			|| field.dataType === FormFieldDataType.WORD_PROCESSOR_PAGE 
			|| field.dataType === FormFieldDataType.WORD_PROCESSOR)
		.forEach(field => {
			Object.keys(field.meta).forEach(key => {
				metadata.set(key, field.meta[key]);
			});
		});
		update.getMetaData().set(metadata); */
		//console.log('ObjectFormController._updateSample metadata', { metadata });
		//update.getMetaData().set("isMarkdown", "true");
		const properties = getChangedEditableFieldValues(form);
		update.setProperties(properties);
		return update;
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
		console.log({ editable, deletable })
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

	async move(form: Form, context?: any, params?: any): Promise<void> {
		const { SampleFetchOptions, SampleUpdate, SpacePermId } = this.openbisFacade;

		if (params.moveDescendants) {
			await this.moveSampleWithDescendants([form.entityPermId], params.target['@type']);
		} else {
			const sampleUpdate = this._prepareSampleUpdate(form.entityPermId, params);

			console.log('ObjectFormController.move', form, context, params);
			const result = await this.openbisFacade.updateSamples([sampleUpdate]);
			console.log('ObjectFormController.move', result);
		}
		return Promise.resolve();
	}

	/**
	 * Prepare the sample update for the move
	 * @param samplePermId - The permId of the sample to update
	 * @param params - The parameters for the move
	 * @returns The sample update
	 */
	_prepareSampleUpdate(samplePermId: string, params: any) {
		const { SampleUpdate, SamplePermId } = this.openbisFacade;
		const sampleUpdate = new SampleUpdate();
		sampleUpdate.setSampleId(new SamplePermId(samplePermId));

		const selectedEntityType = params.target['@type'];
		switch (selectedEntityType) {
			case 'as.dto.project.Project':
				sampleUpdate.setExperimentId(null);
				sampleUpdate.setProjectId(params.target.getPermId());
				sampleUpdate.setSpaceId(params.target.getSpace().getPermId());
				break;
			case 'as.dto.experiment.Experiment':
				sampleUpdate.setSpaceId(params.target.getProject().getSpace().getPermId());
				sampleUpdate.setProjectId(params.target.getProject().getPermId());
				sampleUpdate.setExperimentId(params.target.getPermId());
				break;
			case 'as.dto.space.Space':
				sampleUpdate.setExperimentId(null);
				sampleUpdate.setProjectId(null);
				sampleUpdate.setSpaceId(params.target.getPermId());
				break;
		}
		return sampleUpdate;
	};

	/**
	 * Move samples with descendants
	 * @param permIds - The permIds of the samples to move
	 * @param selectedEntityType - The type of the entity to move to
	 * @returns The void because the implementation is not yet implemented
	 */
	async moveSampleWithDescendants(
		permIds: any[],
		selectedEntityType: string,
	): Promise<void> {
		throw new Error('[ObjectFormController.moveSampleWithDescendants] Not implemented yet.');
		/* 
		// implementation taken from ELN-LIMS
		const { SampleFetchOptions } = this.openbisFacade;
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		fetchOptions.withChildrenUsing(fetchOptions);
	
		const map = await this.openbisFacade.getSamples(permIds, fetchOptions);
		const samplesToUpdate: any[] = [];
		const updates: any[] = [];
	
		for (let i = 0; i < this.moveEntityModel.entities.length; i++) {
		  const entity = this.moveEntityModel.entities[i];
		  const permId = entity.getPermId();
		  this.gatherAllDescendants(samplesToUpdate, map[permId]);
	
		  let level: string;
		  let currentEntity: string;
	
		  if (entity.getExperiment()) {
			level = 'EXPERIMENT';
			currentEntity = entity.getExperiment().getPermId().getPermId();
		  } else if (entity.getProject()) {
			level = 'PROJECT';
			currentEntity = entity.getProject().getPermId().getPermId();
		  } else {
			level = 'SPACE';
			currentEntity = entity.getSpace().getPermId().getPermId();
		  }
	
		  // Filter samples based on current level
		  samplesToUpdate.forEach((sample: any) => {
			let shouldUpdate = false;
	
			switch (level) {
			  case 'EXPERIMENT':
				shouldUpdate = sample.getExperiment() != null &&
				  currentEntity === sample.getExperiment().getPermId().getPermId();
				break;
			  case 'PROJECT':
				shouldUpdate = sample.getExperiment() == null &&
				  currentEntity === sample.getProject().getPermId().getPermId();
				break;
			  case 'SPACE':
				shouldUpdate = sample.getExperiment() == null &&
				  sample.getProject() == null &&
				  currentEntity === sample.getSpace().getPermId().getPermId();
				break;
			}
	
			if (shouldUpdate) {
			  const sampleUpdate = prepareSampleUpdate(sample.getPermId());
			  updates.push(sampleUpdate);
			}
		  });
		}
	
		await this.openbisFacade.updateSamples(updates); */
	}
}