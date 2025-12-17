import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { DatasetFormModel } from '@src/js/components/database/new-forms/entities/Dataset/DatasetFormModel.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { findFormFieldById, getChangedEditableFieldValues } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';

export class DatasetFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
		const { DataSetPermId, DataSetFetchOptions } = this.openbisFacade;
		const id = new DataSetPermId(permId);
		const fetchOptions = new DataSetFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withSample();
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
		/* const objId = form.entityPermId;
		const { ExperimentPermId, SampleIdentifier, DataSetPermId } = this.openbisFacade;
		const experimentId = new ExperimentPermId(objId);
		const collectionIdentifier = findFormFieldById(form.fields, 'identifier')?.value;
		console.log({collectionIdentifier})
		const dummyId = new DataSetPermId(getProjectIdentifierFromExperimentIdentifier(collectionIdentifier) + "/DUMMY_" + guid());
		const dummyId2 = new SampleIdentifier(getProjectIdentifierFromExperimentIdentifier(collectionIdentifier) + "/DUMMY2_" + guid());
		const ids = [experimentId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
		console.log({editable, deletable})
		return { canEdit: editable, canDelete: deletable, canMove: true }; */
		return { canEdit: true, canDelete: true, canMove: true };
	}

	async delete(form: Form, context?: any): Promise<void> {
		console.log(`CONTROLLER: Deleting ${form.entityPermId}`, context);
		/**
	 * this.deleteDataSet = function(reason) {
		var _this = this;
		Util.blockUI();
		mainController.serverFacade.deleteDataSets([this._dataSetFormModel.dataSetV3.code], reason, function(data) {
			if(data.error) {
				Util.showError(data.error.message);
			} else {
				Util.showSuccess("Data Set moved to Trashcan");
				
				setTimeout(function() { //Give some time to update the index
					var space = null;
					if(_this._dataSetFormModel.isExperiment()) {
						mainController.changeView('showExperimentPageFromIdentifier', encodeURIComponent('["' +
								_this._dataSetFormModel.entity.identifier.identifier + '",false]'));
						experimentIdentifier = _this._dataSetFormModel.entity.identifier.identifier;
						space = IdentifierUtil.getSpaceCodeFromIdentifier(experimentIdentifier);
					} else {
						mainController.changeView('showViewSamplePageFromPermId', _this._dataSetFormModel.entity.permId);
						sampleIdentifier = _this._dataSetFormModel.entity.identifier;
						space = IdentifierUtil.getSpaceCodeFromIdentifier(sampleIdentifier);
					}
					
					var isInventory = profile.isInventorySpace(space);
					if(!isInventory) {
						mainController.sideMenu.refreshNodeParentByPermId("DATASET", _this._dataSetFormModel.dataSetV3.code);
					}
				}, 3000);
			}
		});
	}
	 */
	}

	async getDependentEntities(form: Form): Promise<any> {
		// Datasets typically don't have dependent entities
		return {
			datasets: [],
			samples: []
		};
	}

	move(form: Form, context?: any): Promise<void> {
		console.log('DatasetFormController.move', form, context);
		return Promise.resolve();
	}
}