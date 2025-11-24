import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { createDummySampleIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldByLabel } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

export class SpaceFormController implements IFormController {
  private openbisFacade: any;

  constructor(openbisFacade: any) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
  }

  async load(permId: string): Promise<Form> {
    const { SpacePermId, SpaceFetchOptions } = this.openbisFacade;
    const id = new SpacePermId(permId);
    const fetchOptions = new SpaceFetchOptions();
    fetchOptions.withProjects && fetchOptions.withProjects();
    fetchOptions.withRegistrator && fetchOptions.withRegistrator();
    fetchOptions.withSamples && fetchOptions.withSamples();
    const result = await this.openbisFacade.getSpaces([id], fetchOptions);

    const spaceDto = result[permId];

    console.log(spaceDto);
    if (!spaceDto) throw new Error(`Space with permId ${permId} not found`);
    return SpaceFormModel.adaptSpaceDtoToForm(spaceDto);
  }

  async save(form: Form, mode: FormMode): Promise<number> {
    if (mode === FormMode.CREATE) {
      return this._createSpace(form);
    } else if (mode === FormMode.EDIT) {
      return this._updateSpace(form);
    } else {
      throw new Error(`Invalid form mode: ${mode}`);
    }
  }

  async _createSpace(form: Form): Promise<number> {
    throw new Error('Not implemented');
  }

  async _updateSpace(form: Form): Promise<number> {
    const { SpacePermId, SpaceUpdate } = this.openbisFacade;
    const spaceUpdate = new SpaceUpdate()
    spaceUpdate.setSpaceId(new SpacePermId(form.entityPermId));
    const description = findFormFieldByLabel(form.fields, 'Description', true);
    console.log({description})
    spaceUpdate.setDescription(description);
    const result = await this.openbisFacade.updateSpaces([spaceUpdate]);
    console.log('SpaceFormController.updateSpace', result);
    return Promise.resolve(form.version ? form.version + 1 : 1);
  }

  async checkPermissions(form: Form) {
    const { SpacePermId, ProjectIdentifier, SampleIdentifier } = this.openbisFacade;
    const spaceCode = form.entityPermId;
    const spacePermId = new SpacePermId(spaceCode);
    const dummyProjectId = new ProjectIdentifier(createDummySampleIdentifier(spaceCode));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifier(spaceCode));
    console.log({dummyProjectId, dummySampleId})
    const ids = [spacePermId, dummyProjectId, dummySampleId];
    const { editable, deletable } = await fetchRights(this.openbisFacade, spaceCode, ids);
		console.log({editable, deletable})
		//return { canEdit: editable, canDelete: deletable, canMove: true };
    return { canEdit: true, canDelete: true, canMove: true };
  }

  async delete(form: Form, context?: any): Promise<void> {
    // Implement delete logic as needed
    console.log('SpaceFormController.delete', form, context);
  }

  async getDependentEntities(form: Form): Promise<any> {
    // For spaces, check for projects and samples
    const { SpacePermId, SpaceFetchOptions } = this.openbisFacade;
    const id = new SpacePermId(form.entityPermId);
    const fetchOptions = new SpaceFetchOptions();
    fetchOptions.withProjects && fetchOptions.withProjects();
    fetchOptions.withSamples && fetchOptions.withSamples();
    const result = await this.openbisFacade.getSpaces([id], fetchOptions);
    const space = result[form.entityPermId];
    
    return { 
      projects: space.getProjects ? space.getProjects() : [], 
      samples: space.getSamples ? space.getSamples() : [] 
    };
  }

  move(form: Form, context?: any): Promise<void> {
    // Implement move logic as needed
    console.log('SpaceFormController.move', form, context);
    return Promise.resolve();
  }

}