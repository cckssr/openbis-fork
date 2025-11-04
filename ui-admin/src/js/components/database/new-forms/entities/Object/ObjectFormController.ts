import { Form, FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/AuthorizationService.ts';
import { createDummyDataSetIdentifierFromSampleIdentifier, createDummySampleIdentifierFromSampleIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { ObjectFormModel } from '@src/js/components/database/new-forms/entities/Object/ObjectFormModel.ts';

export class ObjectFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	/* var parameters = {
				"method" : "getNextSequenceForType",
				"sampleTypeCode" : sampleType.code
			}
			this.customELNASAPI(parameters, function(nextInSequence) {
				action(sampleType.codePrefix.toUpperCase() + nextInSequence);
			});
	} */

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
		return Promise.resolve(sampleCreation, sample);
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
		const documentField = findFormFieldById(form.fields, form.entityPermId, 'document') as FormField;
		const properties: { [key: string]: string } = {};
		if (documentField) {
			properties['DOCUMENT'] = documentField.value;
			properties['NAME'] = documentField.meta?.title;
		}
		//update.getMetaData().set('MARKDOWN', documentField.meta?.isMarkdown ? 'true' : 'false');
		
		console.log('ObjectFormController._updateSample', { properties });
		update.setProperties(properties);
		return update;
	}

	/* _setBasics(object: any, parameters: any): void {
		const space = parameters["sampleSpace"];
		object.setSpaceId(new SpacePermId(space));
		let sampleIdentifier = "/" + space;
		const project = parameters["sampleProject"];
		if (project != null) {
			object.setProjectId(new ProjectIdentifier("/" + space + "/" + project));
			//if(IdentifierUtil.isProjectSamplesEnabled) {
			sampleIdentifier = sampleIdentifier + "/" + project;
			//}
			const experiment = parameters["sampleExperiment"]
			if (experiment != null) {
				object.setExperimentId(new ExperimentIdentifier("/" + space + "/" + project + "/" + experiment));
			}
		}
		if (object.setSampleId) {
			object.setSampleId(new SampleIdentifier(sampleIdentifier + "/" + parameters["sampleCode"]));
		}
		var sampleProperties = parameters["sampleProperties"];
		var properties = {};
		Object.keys(sampleProperties).forEach(function(key) {
			var sampleProperty = sampleProperties[key];
			if (sampleProperty === "") {
				sampleProperty = null;
			}
			properties[key] = sampleProperty;
		});
		object.setProperties(properties);
	} */

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
	
		const sampleUpdate = this._prepareSampleUpdate(form.entityPermId, params);

		console.log('ObjectFormController.move', form, context, params);
		const result = await this.openbisFacade.updateSamples([sampleUpdate]);
		return Promise.resolve();
	}

	_prepareSampleUpdate(samplePermId: any, params: any) {
		const { SampleUpdate } = this.openbisFacade;
		const sampleUpdate = new SampleUpdate();
		sampleUpdate.setSampleId(samplePermId);
  
		const selectedEntityType = params.moveEntityModel.selected['@type'];
		switch (selectedEntityType) {
		  case 'as.dto.project.Project':
			sampleUpdate.setExperimentId(null);
			sampleUpdate.setProjectId(params.moveEntityModel.selected.getPermId());
			sampleUpdate.setSpaceId(params.moveEntityModel.selected.getSpace().getPermId());
			break;
		  case 'as.dto.experiment.Experiment':
			sampleUpdate.setSpaceId(params.moveEntityModel.selected.getProject().getSpace().getPermId());
			sampleUpdate.setProjectId(params.moveEntityModel.selected.getProject().getPermId());
			sampleUpdate.setExperimentId(params.moveEntityModel.selected.getPermId());
			break;
		  case 'as.dto.space.Space':
			sampleUpdate.setExperimentId(null);
			sampleUpdate.setProjectId(null);
			sampleUpdate.setSpaceId(params.moveEntityModel.selected.getPermId());
			break;
		}
		return sampleUpdate;
	  };

	/* private async moveSample(descendants: boolean): Promise<void> {
		const { SampleFetchOptions, SampleUpdate, SpacePermId } = this.openbisFacade;
	
		const sampleUpdate = this._prepareSampleUpdate(form.entityPermId);

		const permIds = this.moveEntityModel.entities.map(x => x.getPermId());
		const selectedEntityType = this.moveEntityModel.selected['@type'];
	
		if (descendants) {
		  await this.moveSampleWithDescendants(permIds, selectedEntityType, prepareSampleUpdate);
		} else {
		  const sampleUpdates = permIds.map(x => prepareSampleUpdate(x));
		  await this.openbisFacade.updateSamples(sampleUpdates);
		}
	  }

	async moveSampleWithDescendants(
		permIds: any[],
		selectedEntityType: string,
		prepareSampleUpdate: (permId: any) => any
	  ): Promise<void> {
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
	
		await this.openbisFacade.updateSamples(updates);
	  } */
}