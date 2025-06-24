import { Form, FormFieldDataType } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.ts';

/* const rawSampleDto = {
	"@type": "as.dto.sample.Sample",
	"permId": { "permId": "20250612-SAMPLE-01" },
	"identifier": { "identifier": "/MY_SPACE/PROJECT_X/S1" },
	"code": "S1",
	"registrationDate": 1749631136335,
	"modificationDate": 1749631136335,
	"properties": {
		"DESCRIPTION": "This is a detailed description from properties.",
		"NOTES": "Handle with care."
	},
	"type": { "code": "CELL_CULTURE" },
	"version": 12,
}; */

function adaptSampleDtoToForm(dto: any): Form {
	return {
		entityPermId: dto.permId.permId,
		entityType: dto.type.code,
		title: `Sample: ${dto.code}`,
		version: dto.version,
		entityKind: 'SAMPLE',
		meta: {},
		fields: [
			{ id: 'code', label: 'Code', value: dto.code, dataType: FormFieldDataType.VARCHAR, isMandatory: true, isMultiValue: false, meta: [] },
			{ id: 'DESCRIPTION', label: 'Description', value: dto.properties.DESCRIPTION, dataType: FormFieldDataType.VARCHAR, isMandatory: false, isMultiValue: false, meta: []  },
			{ id: 'NOTES', label: 'Notes', value: dto.properties.NOTES, dataType: FormFieldDataType.VARCHAR, isMandatory: false, isMultiValue: false, meta: []  },
			{ id: 'registrationDate', label: 'Registration Date', value: new Date(dto.registrationDate).toLocaleDateString(), dataType: FormFieldDataType.TIMESTAMP, isMandatory: false, isMultiValue: false, meta: []  }
		]
	};
}

export class SampleFormController implements FormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
        //const formModel = adaptSampleDtoToForm(rawSampleDto);
        //return Promise.resolve(formModel);
		// Fetch the sample from openBIS using openbisFacade
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
		// You can add more fetch options as needed
		const result = await this.openbisFacade.searchSamples(criteria, fetchOptions);
		const sampleDto = Object.values(result.objects)[0];
		if (!sampleDto) throw new Error(`Sample with permId ${permId} not found`);
		return adaptSampleDtoToForm(sampleDto);
	}

	async save(form: Form): Promise<number> {
		console.log('--- CONTROLLER: SAVING FORM ---');
		console.log(JSON.stringify(form, null, 2));
		console.log('-----------------------------');
		// Simulate a successful save by returning an incremented version number.
		return Promise.resolve(form.version + 1);
	}

	async checkPermissions(form: Form) {
		return { canEdit: true, canDelete: true, canMove: true };
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