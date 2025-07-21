import { Form, FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/types/FormController';
import { createDummySampleIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/AuthorizationService.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts';

export class SpaceFormController implements FormController {
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

  async save(form: Form): Promise<number> {
    const { SpacePermId, SpaceUpdate } = this.openbisFacade;
    const spaceUpdate = new SpaceUpdate()
    spaceUpdate.setSpaceId(new SpacePermId(form.entityPermId));
    const description = findFormFieldById(form.fields, form.entityPermId + '-description')?.value;
    spaceUpdate.setDescription(description);
    const result = await this.openbisFacade.updateSpaces([spaceUpdate]);
    console.log(result)
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

  edit(form: Form): void {
    // Implement edit logic as needed
  }

  delete(form: Form): void {
    // Implement delete logic as needed
  }

  move(form: Form): void {
    // Implement move logic as needed
  }

}