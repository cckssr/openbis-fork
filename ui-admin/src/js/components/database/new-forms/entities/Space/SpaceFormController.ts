import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { createDummySampleIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldByLabel } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';

export class SpaceFormController implements IFormController {
  private openbisFacade: any;
  private deleteService: DeleteService;

  constructor(openbisFacade: any) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
    this.deleteService = new DeleteService({ openbisFacade: this.openbisFacade });
  }

  async load(permId: string, entityKind?: string): Promise<Form> {
    if (entityKind === EntityKind.NEW_SPACE) {
      return SpaceFormModel.adaptNewSpaceDtoToForm(permId);
    }

    const { SpacePermId, SpaceFetchOptions } = this.openbisFacade;
    const id = new SpacePermId(permId);
    const fetchOptions = new SpaceFetchOptions();
    fetchOptions.withProjects && fetchOptions.withProjects();
    fetchOptions.withRegistrator && fetchOptions.withRegistrator();
    fetchOptions.withModifier && fetchOptions.withModifier();
    fetchOptions.withSamples && fetchOptions.withSamples();
    const result = await this.openbisFacade.getSpaces([id], fetchOptions);

    const spaceDto = result[permId];

    console.log({spaceDto});
    if (!spaceDto) throw new Error(`Space with permId ${permId} not found`);
    return SpaceFormModel.adaptSpaceDtoToForm(spaceDto);
  }

  async save(form: Form, mode: FormMode): Promise<any> {
    if (mode === FormMode.CREATE) {
      return this._createSpace(form);
    } else if (mode === FormMode.EDIT) {
      return this._updateSpace(form);
    } else {
      throw new Error(`Invalid form mode: ${mode}`);
    }
  }

  async _createSpace(form: Form): Promise<string> {
    const { SpaceCreation } = this.openbisFacade;
    const creation = new SpaceCreation();
    creation.setCode(findFormFieldByLabel(form.fields, 'Code', true));
    creation.setDescription(findFormFieldByLabel(form.fields, 'Description', true));
    const result = await this.openbisFacade.createSpaces([creation]);
    return result[0].getPermId();
  }

  async _updateSpace(form: Form): Promise<number> {
    const { SpacePermId, SpaceUpdate } = this.openbisFacade;
    const spaceUpdate = new SpaceUpdate()
    spaceUpdate.setSpaceId(new SpacePermId(form.entityPermId));
    const description = findFormFieldByLabel(form.fields, 'Description', true);
    spaceUpdate.setDescription(description);
    const result = await this.openbisFacade.updateSpaces([spaceUpdate]);
    return Promise.resolve(form.version ? form.version + 1 : 1);
  }

  async checkPermissions(form: Form) {
    const { SpacePermId, ProjectIdentifier, SampleIdentifier } = this.openbisFacade;
    const spaceCode = form.entityPermId;
    const spacePermId = new SpacePermId(spaceCode);
    const dummyProjectId = new ProjectIdentifier(createDummySampleIdentifier(spaceCode));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifier(spaceCode));
    const ids = [spacePermId, dummyProjectId, dummySampleId];
    const { editable, deletable } = await fetchRights(this.openbisFacade, spaceCode, ids);
    //return { canEdit: editable, canDelete: deletable, canMove: true };
    return { canEdit: true, canDelete: true, canMove: true };
  }

  async delete(form: Form, context?: any): Promise<void> {
    const spaceCode = form.entityPermId;
    
    // Check for existing deletions in trashcan before proceeding using DeleteService
    const dependentDeletions = await this.deleteService.checkExistingDeletions(
      spaceCode,
      'SPACE',
      ['SAMPLE']
    );
    if (dependentDeletions.length > 0) {
      const errorMessage = this.deleteService.formatDeletionError(dependentDeletions, 'space');
      throw new Error(errorMessage);
    }
    
    // Check for dependent entities to validate space is empty
    const dependentEntities = await this.getDependentEntities(form);
    const projectsCount = dependentEntities.projects?.length || 0;
    const samplesCount = dependentEntities.samples?.length || 0;
    const totalDependentEntities = projectsCount + samplesCount;

    // If this is just a check, validate and return early
    if (context?.checkOnly) {
      if (totalDependentEntities > 0) {
        throw new Error(
          `Cannot delete space: Space is not empty. It contains ${projectsCount} project(s) and ${samplesCount} sample(s). Please delete or move these entities first.`
        );
      }
      return;
    }

    // Prevent deletion if space is not empty
    if (totalDependentEntities > 0) {
      throw new Error(
        `Cannot delete space: Space is not empty. It contains ${projectsCount} project(s) and ${samplesCount} sample(s). Please delete or move these entities first.`
      );
    }

    // Space is empty, proceed with deletion (moves to trashcan) using DeleteService
    const deleteReason = context?.deleteReason || 'delete via ng-ui';
    const result = await this.deleteService.moveSpacesToTrashcan([spaceCode], deleteReason);
    if (!result.success) {
      throw new Error(result.error || 'Failed to move space to trashcan');
    }
    return Promise.resolve();
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


  async move(form: Form, context?: any, params?: any): Promise<void> {
    // Spaces are root-level entities and cannot be moved
    throw new Error('Spaces cannot be moved. They are root-level entities in the openBIS hierarchy.');
  }

}