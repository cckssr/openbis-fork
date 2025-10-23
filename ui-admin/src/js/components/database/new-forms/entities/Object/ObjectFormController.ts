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

	move(form: Form): void {
		console.log(`CONTROLLER: Moving ${form.entityPermId}`);
	}
}