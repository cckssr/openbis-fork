import openbis from '@srcV3/openbis.esm';
import { Form, IExtendedActionContext, } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyExperimentIdentifierFromProjectIdentifier, createDummySampleIdentifierFromProjectIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById, findFormFieldByLabel } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';
import { MoveService } from '@src/js/components/database/new-forms/services/MoveService.ts';

export class ProjectFormController implements IFormController {
  private openbisFacade: openbis.openbis;
  private spacePermId: string = '';
  private deleteService: DeleteService;
  private moveService: MoveService;

  constructor(openbisFacade: openbis.openbis) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
    this.deleteService = new DeleteService({ openbisFacade: this.openbisFacade });
    this.moveService = new MoveService({ openbisFacade: this.openbisFacade });
  }

  async load(permId: string, entityKind?: string, params?: any): Promise<Form> {
    if (entityKind === EntityKind.NEW_PROJECT) {
      return ProjectFormModel.adaptNewProjectDtoToForm(permId, params);
    } else {
      const { ProjectPermId, ProjectFetchOptions, ExperimentIdentifier, RightsFetchOptions } = this.openbisFacade;
      const id = new ProjectPermId(permId);
      const fetchOptions = new ProjectFetchOptions();
      fetchOptions.withSpace();
      fetchOptions.withModifier();
      fetchOptions.withRegistrator();
      const result = await this.openbisFacade.getProjects([id], fetchOptions);

      const projectDto = result[permId];
      if (!projectDto) throw new Error(`Project with permId ${permId} not found`);
      /* const spaceCode = projectDto.getSpace().getCode();
      const projectCode = projectDto.getCode();
      
      const sessionInfo = await this.openbisFacade.getSessionInformation();
      console.log({ sessionInfo })
      console.log({ spaceCode }, { projectCode });
      const roles = await getUserRole(this.openbisFacade, false, spaceCode, projectCode);
      console.log({roles}); */
      return ProjectFormModel.adaptProjectDtoToForm(projectDto);
    }
  }

  async save(form: Form, mode: FormMode): Promise<any> {
    if (mode === FormMode.CREATE) {
      return this._createProject(form);
    } else if (mode === FormMode.EDIT) {
      return this._updateProject(form);
    } else {
      throw new Error(`Invalid form mode: ${mode}`);
    }
  }

  async checkPermissions(form: Form) {
    const { ProjectPermId, ExperimentIdentifier, SampleIdentifier } = this.openbisFacade;
    const projectCode = form.entityPermId;
    const projectPermId = new ProjectPermId(projectCode);
    const projectIdentifierField = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
    if (!projectIdentifierField || typeof projectIdentifierField !== 'string') throw new Error('Project identifier not found');
    const projectIdentifier = projectIdentifierField;
    const dummyExperimentId = new ExperimentIdentifier(createDummyExperimentIdentifierFromProjectIdentifier(projectIdentifier));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifierFromProjectIdentifier(projectIdentifier));
    const ids = [projectPermId, dummyExperimentId, dummySampleId];
    const { editable, deletable } = await fetchRights(this.openbisFacade, projectCode, ids);
    return { canEdit: editable, canDelete: deletable, canMove: true };
    //return { canEdit: true, canDelete: true, canMove: true };
  }

  async delete(form: Form, context?: any): Promise<void> {
    // Check for existing deletions in trashcan before proceeding
    const projectIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);

    if (!projectIdentifier || typeof projectIdentifier !== 'string') throw new Error('Project identifier not found');

    const dependentDeletions = await this.deleteService.checkExistingDeletions(
      projectIdentifier,
      'PROJECT',
      ['EXPERIMENT', 'SAMPLE']
    );
    if (dependentDeletions.length > 0) {
      const errorMessage = this.deleteService.formatDeletionError(dependentDeletions, 'project');
      throw new Error(errorMessage);
    }

    // If this is just a check, return early
    if (context?.checkOnly) {
      return;
    }

    // Get dependent entities if not provided in context
    // Use rawDependentEntities from context if available (from normalized structure)
    let dependentEntities = context?.rawDependentEntities || context?.dependentEntities;
    if (!dependentEntities) {
      dependentEntities = await this.getDependentEntities(form);
    }

    // Get delete reason from context or use default
    const deleteReason = context?.deleteReason || 'delete via ng-ui';

    // Check if project is empty
    const isEmpty = (!dependentEntities.experiments || dependentEntities.experiments.length === 0) &&
      (!dependentEntities.samples || dependentEntities.samples.length === 0);

    if (!isEmpty) {
      // Non-empty project: Move all entities to trashcan and STOP (don't delete the project)
      // Move dependent entities to trashcan using DeleteService
      let movedCount = 0;
      if (dependentEntities.experiments && dependentEntities.experiments.length > 0) {
        const result = await this.deleteService.moveExperimentsToTrashcan(dependentEntities.experiments, deleteReason);
        if (!result.success) {
          throw new Error(result.error || 'Failed to move experiments to trashcan');
        }
        movedCount += result.count;
      }

      if (dependentEntities.samples && dependentEntities.samples.length > 0) {
        const result = await this.deleteService.moveSamplesToTrashcan(dependentEntities.samples, deleteReason);
        if (!result.success) {
          throw new Error(result.error || 'Failed to move samples to trashcan');
        }
        movedCount += result.count;
      }
      
      return Promise.resolve();
    }

    // Empty project: Move the project itself to trashcan using DeleteService
    const result = await this.deleteService.moveProjectsToTrashcan(
      [{ identifier: projectIdentifier }],
      deleteReason
    );
    if (!result.success) {
      throw new Error(result.error || 'Failed to move project to trashcan');
    }
    return Promise.resolve();
  }

  async getDependentEntities(form: Form): Promise<any> {
    const { ProjectPermId, ProjectFetchOptions } = this.openbisFacade;
    const id = new ProjectPermId(form.entityPermId);
    const fetchOptions = new ProjectFetchOptions();
    fetchOptions.withExperiments();
    fetchOptions.withSamples().withExperiment();
    const result = await this.openbisFacade.getProjects([id], fetchOptions);
    const project = result[id];
    return { experiments: project.getExperiments(), samples: project.getSamples() };
  }

  /**
   * @deprecated Use moveEntitiesToTrashcan instead. This method actually deletes entities.
   * Kept for backward compatibility but should not be used for non-empty projects.
   */
  async deleteDependentEntities(reason: string, dependentEntities: any): Promise<void> {
    const { ExperimentIdentifier, SampleIdentifier, ExperimentDeletionOptions, SampleDeletionOptions } = this.openbisFacade;
    // Delete experiments first
    if (dependentEntities.experiments && dependentEntities.experiments.length > 0) {
      const experimentIds = dependentEntities.experiments.map((exp: any) => new ExperimentIdentifier(exp.getIdentifier().getIdentifier()));
      const experimentDeletionOptions = new ExperimentDeletionOptions();
      experimentDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteExperiments(experimentIds, experimentDeletionOptions);
    }
    // Then delete independent samples (samples not associated with experiments we just deleted)
    if (dependentEntities.samples && dependentEntities.samples.length > 0) {
      const experimentIds = new Set((dependentEntities.experiments || []).map((exp: any) => exp.getPermId()));
      const independentSamples = dependentEntities.samples
        .filter((sample: any) => {
          const experiment = sample.getExperiment();
          return !experiment || !experimentIds.has(experiment.getPermId());
        })
        .map((sample: any) => new SampleIdentifier(sample.getIdentifier().getIdentifier()));

      if (independentSamples.length > 0) {
        const sampleDeletionOptions = new SampleDeletionOptions();
        sampleDeletionOptions.setReason(reason);
        await this.openbisFacade.deleteSamples(independentSamples, sampleDeletionOptions);
      }
    }
  }

  async move(form: Form, context?: any, params?: any): Promise<void> {
    if (!params || !params.target) {
      throw new Error('Target is required for move operation');
    }

    const result = await this.moveService.moveProject(form.entityPermId, params.target.getPermId());

    if (!result.success) {
      throw new Error(result.error || 'Failed to move project');
    }
    return Promise.resolve();
  }

  async _createProject(form: Form): Promise<any> {
    const { ProjectCreation, SpacePermId } = this.openbisFacade;
    const creation = new ProjectCreation();
    creation.setCode(findFormFieldByLabel(form.fields, 'Code', true));
    creation.setSpaceId(new SpacePermId(form.meta.spacePermId));
    creation.setDescription(findFormFieldByLabel(form.fields, 'Description', true));
    const result = await this.openbisFacade.createProjects([creation]);
    return result[0].getPermId();
  }

  async _updateProject(form: Form): Promise<any> {
    const { ProjectPermId, ProjectUpdate } = this.openbisFacade;
    const projectUpdate = new ProjectUpdate();
    projectUpdate.setProjectId(new ProjectPermId(form.entityPermId));
    projectUpdate.setDescription(findFormFieldByLabel(form.fields, 'Description', true));
    const result = await this.openbisFacade.updateProjects([projectUpdate]);
    return Promise.resolve(form.version ? form.version + 1 : 1);
  }
}
