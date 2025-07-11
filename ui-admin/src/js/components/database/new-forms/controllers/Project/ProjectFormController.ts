import openbis from '@srcV3/openbis.esm';
import { Form, findFormFieldById } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.ts';
import { adaptProjectDtoToForm } from '@src/js/components/database/new-forms/adapters/entity.adapter.ts';
import { fetchRights } from '@src/js/components/database/new-forms/controllers/AuthorizationService.ts';
import { guid } from '@src/js/components/database/new-forms/Utils.ts';

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
    const spaceCode = projectDto.space.code;
    const projectCode = projectDto.code;
    
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
    const objId = form.entityPermId;
    const projectId = new ProjectPermId(objId);
    const projectIdentifier = findFormFieldById(form.fields, 'identifier')?.value;
    console.log({projectIdentifier})
    const dummyId = new ExperimentIdentifier(projectIdentifier + "/DUMMY_" + guid());
    const dummyId2 = new SampleIdentifier(projectIdentifier + "/DUMMY2_" + guid());
    const ids = [projectId, dummyId, dummyId2];
		const { editable, deletable } = await fetchRights(this.openbisFacade, objId, ids);
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
