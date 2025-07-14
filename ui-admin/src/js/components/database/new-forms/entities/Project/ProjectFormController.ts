import openbis from '@srcV3/openbis.esm';
import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/entities/FormController.ts';
import { adaptProjectDtoToForm } from '@src/js/components/database/new-forms/adapters/entity.adapter.ts';
import { fetchRights } from '@src/js/components/database/new-forms/entities/AuthorizationService.ts';
import { createDummyExperimentIdentifierFromProjectIdentifier, createDummySampleIdentifierFromProjectIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';

export class ProjectFormController implements FormController {
  private openbisFacade: openbis.openbis;
  private spacePermId: string = '';

  constructor(openbisFacade: openbis.openbis) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
  }

  async load(permId: string): Promise<Form> {

    const { ProjectPermId, ProjectFetchOptions, ExperimentIdentifier, RightsFetchOptions } = this.openbisFacade;
    const id = new ProjectPermId(permId);
    const fetchOptions = new ProjectFetchOptions();
    fetchOptions.withSpace();

    const result = await this.openbisFacade.getProjects([id], fetchOptions);

    const projectDto = result[permId];
    console.log({ projectDto });
    if (!projectDto) throw new Error(`Project with permId ${permId} not found`);
    const spaceCode = projectDto.getSpace().getCode();
    const projectCode = projectDto.getCode();
    
    const sessionInfo = await this.openbisFacade.getSessionInformation();
    console.log({ sessionInfo })
    console.log({ spaceCode }, { projectCode });
    return adaptProjectDtoToForm(projectDto);
  }

  async save(form: Form): Promise<number> {
    return Promise.resolve(form.version + 1);
  }

  async checkPermissions(form: Form) {
    const { ProjectPermId, ExperimentIdentifier, SampleIdentifier } = this.openbisFacade;
    const projectCode = form.entityPermId;
    const projectPermId = new ProjectPermId(projectCode);
    const projectIdentifier = findFormFieldById(form.fields, 'identifier')?.value;
    console.log({projectIdentifier})
    const dummyExperimentId = new ExperimentIdentifier(createDummyExperimentIdentifierFromProjectIdentifier(projectIdentifier));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifierFromProjectIdentifier(projectIdentifier));
    const ids = [projectPermId, dummyExperimentId, dummySampleId];
		const { editable, deletable } = await fetchRights(this.openbisFacade, projectCode, ids);
		return { canEdit: editable, canDelete: deletable, canMove: true };
    //return { canEdit: true, canDelete: true, canMove: true };
  }


  delete(form: Form): void {
    // Implement delete logic as needed
  }

  move(form: Form): void {
    // Implement move logic as needed
  }

  create(form: Form): void {
    console.log('ProjectFormController.create', form);
    const { ProjectIdentifier, ProjectCreation } = this.openbisFacade;
    const creation = new ProjectCreation();
    creation.setCode(form.code);
    creation.setSpaceId(form.space);
    creation.setDescription(form.description);
    this.openbisFacade.createProjects([creation]).then(map => {
      console.log('ProjectFormController.create', map);
    });
   
  }
}
